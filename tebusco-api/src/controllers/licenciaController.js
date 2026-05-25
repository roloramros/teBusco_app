import { query, getClient } from '../config/database.js'
import { success, badRequest, notFound } from '../utils/response.js'
import { sendNotification } from '../services/notificationService.js'

/**
 * GET /api/admin/licencias
 * Listar todas las licencias con info del chofer.
 * Soporta filtros: estado, provincia_id, alerta (vence en 7 días)
 */
export const getLicencias = async (req, res, next) => {
  try {
    const { estado, provincia_id, alerta, page = 1, limit = 20 } = req.query
    const offset = (page - 1) * limit
    const safeLimit = Math.min(parseInt(limit), 50)

    let whereClauses = []
    let params = []

    if (estado) {
      whereClauses.push(`l.estado = $${params.length + 1}`)
      params.push(estado)
    }
    if (provincia_id) {
      whereClauses.push(`p.id = $${params.length + 1}`)
      params.push(provincia_id)
    }
    // alerta=1 → licencias que vencen en los próximos 7 días
    if (alerta === '1' || alerta === 'true') {
      whereClauses.push(`(
        (l.estado = 'TRIAL_ACTIVO' AND l.trial_fin BETWEEN NOW() AND NOW() + INTERVAL '7 days')
        OR
        (l.estado = 'ACTIVO' AND l.suscripcion_fin BETWEEN NOW() AND NOW() + INTERVAL '7 days')
      )`)
    }

    const whereSQL = whereClauses.length > 0 ? `WHERE ${whereClauses.join(' AND ')}` : ''

    const sql = `
      SELECT
        l.chofer_id AS id, l.estado, l.trial_inicio, l.trial_fin,
        l.suscripcion_inicio, l.suscripcion_fin,
        l.ultimo_pago, l.monto_mensual, l.saldo_fondo, l.notas,
        l.creada_en, l.actualizada_en,
        c.id AS chofer_id,
        u.id AS usuario_id, u.nombre, u.username, u.telefono,
        u.verificado, u.fcm_token,
        p.nombre AS provincia,
        -- Días restantes según el estado
        CASE
          WHEN l.estado = 'TRIAL_ACTIVO'
            THEN GREATEST(0, EXTRACT(DAY FROM l.trial_fin - NOW())::int)
          WHEN l.estado = 'ACTIVO'
            THEN GREATEST(0, EXTRACT(DAY FROM l.suscripcion_fin - NOW())::int)
          ELSE 0
        END AS dias_restantes
      FROM licencias_chofer l
      JOIN choferes c ON c.id = l.chofer_id
      JOIN usuarios u ON u.id = c.usuario_id
      LEFT JOIN municipios m ON m.id = c.municipio_base_id
      LEFT JOIN provincias p ON p.id = m.provincia_id
      ${whereSQL}
      ORDER BY l.actualizada_en DESC
      LIMIT $${params.length + 1} OFFSET $${params.length + 2}
    `

    const countSQL = `
      SELECT COUNT(*)
      FROM licencias_chofer l
      JOIN choferes c ON c.id = l.chofer_id
      JOIN usuarios u ON u.id = c.usuario_id
      LEFT JOIN municipios m ON m.id = c.municipio_base_id
      LEFT JOIN provincias p ON p.id = m.provincia_id
      ${whereSQL}
    `

    const [{ rows: data }, { rows: countRows }] = await Promise.all([
      query(sql, [...params, safeLimit, offset]),
      query(countSQL, params)
    ])

    return success(res, {
      data,
      total: parseInt(countRows[0].count),
      page: parseInt(page),
      limit: safeLimit
    })
  } catch (err) {
    next(err)
  }
}

/**
 * GET /api/admin/licencias/:chofer_id
 * Ver la licencia de un chofer específico
 */
export const getLicenciaByChofer = async (req, res, next) => {
  try {
    const { chofer_id } = req.params

    const { rows } = await query(`
      SELECT
        l.*,
        u.nombre, u.username, u.telefono, u.verificado, u.fcm_token,
        p.nombre AS provincia,
        CASE
          WHEN l.estado = 'TRIAL_ACTIVO'
            THEN GREATEST(0, EXTRACT(DAY FROM l.trial_fin - NOW())::int)
          WHEN l.estado = 'ACTIVO'
            THEN GREATEST(0, EXTRACT(DAY FROM l.suscripcion_fin - NOW())::int)
          ELSE 0
        END AS dias_restantes
      FROM licencias_chofer l
      JOIN choferes c ON c.id = l.chofer_id
      JOIN usuarios u ON u.id = c.usuario_id
      LEFT JOIN municipios m ON m.id = c.municipio_base_id
      LEFT JOIN provincias p ON p.id = m.provincia_id
      WHERE l.chofer_id = $1
    `, [chofer_id])

    if (rows.length === 0) return notFound(res, 'Licencia no encontrada')
    return success(res, rows[0])
  } catch (err) {
    next(err)
  }
}

/**
 * POST /api/admin/licencias/:chofer_id/registrar-pago
 * El admin registra un depósito en el monedero del chofer.
 * Body: { monto, notas, monto_mensual }
 *
 * Lógica:
 *   - Suma el 'monto' al saldo_fondo.
 *   - Si el chofer estaba SUSPENDIDO o TRIAL_EXPIRADO y el nuevo saldo alcanza para un mes, descuenta el mes, activa y extiende.
 */
export const registrarPago = async (req, res, next) => {
  const client = await getClient()
  try {
    const { chofer_id } = req.params
    const { monto = 0, monto_mensual, notas } = req.body

    const deposito = parseFloat(monto)
    if (isNaN(deposito) || deposito < 0) {
      return badRequest(res, 'El monto a depositar debe ser un número válido mayor o igual a 0')
    }

    await client.query('BEGIN')

    const { rows: licRows } = await client.query(
      `SELECT l.*, c.usuario_id, u.fcm_token, u.nombre
       FROM licencias_chofer l
       JOIN choferes c ON c.id = l.chofer_id
       JOIN usuarios u ON u.id = c.usuario_id
       WHERE l.chofer_id = $1 FOR UPDATE`,
      [chofer_id]
    )

    if (licRows.length === 0) {
      await client.query('ROLLBACK')
      return notFound(res, 'Licencia no encontrada para este chofer')
    }

    const licencia = licRows[0]
    let nuevoSaldo = parseFloat(licencia.saldo_fondo || 0) + deposito
    const cuotaMensual = parseFloat(monto_mensual !== undefined ? monto_mensual : (licencia.monto_mensual || 0))

    let estado = licencia.estado
    let suscripcionInicio = licencia.suscripcion_inicio
    let suscripcionFin = licencia.suscripcion_fin
    let activadoAhora = false

    // Si no está activo (o trial), intentamos cobrar y activar
    const inactivo = ['SUSPENDIDO'].includes(estado)
    if (inactivo && nuevoSaldo >= cuotaMensual && cuotaMensual > 0) {
      nuevoSaldo -= cuotaMensual
      estado = 'ACTIVO'
      suscripcionInicio = new Date()
      
      // Extender 1 mes desde hoy
      const hoy = new Date()
      suscripcionFin = new Date(hoy.setMonth(hoy.getMonth() + 1))
      activadoAhora = true
    }

    await client.query(
      `UPDATE licencias_chofer SET
        estado             = $1,
        suscripcion_inicio = $2,
        suscripcion_fin    = $3,
        saldo_fondo        = $4,
        monto_mensual      = $5,
        ultimo_pago        = NOW(),
        notas              = COALESCE($6, notas),
        actualizada_en     = NOW()
       WHERE chofer_id     = $7`,
      [estado, suscripcionInicio, suscripcionFin, nuevoSaldo, cuotaMensual, notas || null, chofer_id]
    )

    if (activadoAhora) {
      await client.query('UPDATE usuarios SET verificado = true WHERE id = $1', [licencia.usuario_id])
      await client.query(`UPDATE choferes SET estado = 'disponible' WHERE id = $1 AND estado = 'inactivo'`, [chofer_id])
    }

    await client.query('COMMIT')

    // Notificar
    if (activadoAhora) {
      sendNotification({
        usuario_id: licencia.usuario_id,
        tipo: 'sistema_alerta',
        titulo: '✅ Licencia activada',
        cuerpo: `Tu licencia de Te Busco ha sido activada por 1 mes. Saldo restante: $${nuevoSaldo.toFixed(2)}.`,
        fcm_token: licencia.fcm_token
      }).catch(console.error)
    } else if (deposito > 0) {
      sendNotification({
        usuario_id: licencia.usuario_id,
        tipo: 'sistema_alerta',
        titulo: '💰 Depósito recibido',
        cuerpo: `Se han depositado $${deposito.toFixed(2)} en tu fondo. Saldo actual: $${nuevoSaldo.toFixed(2)}.`,
        fcm_token: licencia.fcm_token
      }).catch(console.error)
    }

    return success(res, null, activadoAhora ? 'Licencia reactivada y fondo actualizado.' : 'Fondo actualizado correctamente.')
  } catch (err) {
    await client.query('ROLLBACK')
    next(err)
  } finally {
    client.release()
  }
}

/**
 * POST /api/admin/licencias/:chofer_id/cambiar-estado
 * El admin cambia manualmente el estado de una licencia.
 * Body: { estado, notas }
 * Útil para: bloquear manualmente, extender trial, reactivar, etc.
 */
export const cambiarEstado = async (req, res, next) => {
  const client = await getClient()
  try {
    const { chofer_id } = req.params
    const { estado, notas } = req.body

    const estadosValidos = ['TRIAL_ACTIVO', 'ACTIVO', 'SUSPENDIDO', 'BLOQUEADO']
    if (!estadosValidos.includes(estado)) {
      return badRequest(res, `Estado inválido. Válidos: ${estadosValidos.join(', ')}`)
    }

    await client.query('BEGIN')

    const { rows: licRows } = await client.query(
      `SELECT l.*, c.usuario_id, u.fcm_token
       FROM licencias_chofer l
       JOIN choferes c ON c.id = l.chofer_id
       JOIN usuarios u ON u.id = c.usuario_id
       WHERE l.chofer_id = $1`,
      [chofer_id]
    )

    if (licRows.length === 0) {
      await client.query('ROLLBACK')
      return notFound(res, 'Licencia no encontrada')
    }

    const licencia = licRows[0]

    // Si es TRIAL_ACTIVO, extender trial_fin a 45 días desde hoy
    const extraUpdate = estado === 'TRIAL_ACTIVO'
      ? `, trial_inicio = NOW(), trial_fin = NOW() + INTERVAL '45 days'`
      : ''

    await client.query(
      `UPDATE licencias_chofer SET
         estado = $1,
         notas = COALESCE($2, notas),
         actualizada_en = NOW()
         ${extraUpdate}
       WHERE chofer_id = $3`,
      [estado, notas || null, chofer_id]
    )

    // Sincronizar verificado según el nuevo estado
    const puedeOperar = ['TRIAL_ACTIVO', 'ACTIVO'].includes(estado)
    await client.query(
      'UPDATE usuarios SET verificado = $1 WHERE id = $2',
      [puedeOperar, licencia.usuario_id]
    )

    // Sincronizar estado del chofer
    if (!puedeOperar) {
      await client.query(
        `UPDATE choferes SET estado = 'inactivo' WHERE id = $1`,
        [chofer_id]
      )
    }

    await client.query('COMMIT')

    // Notificaciones según el nuevo estado
    const mensajes = {
      BLOQUEADO: {
        titulo: '🚫 Licencia bloqueada',
        cuerpo: 'Tu licencia de Te Busco ha sido bloqueada. Contacta al soporte para más información.'
      },
      SUSPENDIDO: {
        titulo: '⚠️ Licencia suspendida',
        cuerpo: 'Tu suscripción ha vencido. Renueva tu licencia para volver a operar en Te Busco.'
      },
      TRIAL_ACTIVO: {
        titulo: '🎉 Trial extendido',
        cuerpo: 'Tu período de prueba ha sido extendido por 45 días. ¡Sigue usando Te Busco!'
      },
      ACTIVO: {
        titulo: '✅ Licencia reactivada',
        cuerpo: 'Tu licencia ha sido reactivada manualmente. Ya puedes operar con normalidad.'
      }
    }

    if (mensajes[estado]) {
      sendNotification({
        usuario_id: licencia.usuario_id,
        tipo: 'sistema_alerta',
        ...mensajes[estado],
        fcm_token: licencia.fcm_token
      }).catch(console.error)
    }

    return success(res, null, `Estado de licencia actualizado a ${estado}`)
  } catch (err) {
    await client.query('ROLLBACK')
    next(err)
  } finally {
    client.release()
  }
}

/**
 * GET /api/admin/licencias/stats
 * Resumen de licencias para el dashboard
 */
export const getLicenciasStats = async (req, res, next) => {
  try {
    const { rows } = await query(`
      SELECT
        COUNT(*) FILTER (WHERE estado = 'TRIAL_ACTIVO')   AS trial_activo,
        COUNT(*) FILTER (WHERE estado = 'ACTIVO')         AS activo,
        COUNT(*) FILTER (WHERE estado = 'SUSPENDIDO')     AS suspendido,
        COUNT(*) FILTER (WHERE estado = 'BLOQUEADO')      AS bloqueado,
        COUNT(*) FILTER (WHERE
          estado = 'TRIAL_ACTIVO'
          AND trial_fin BETWEEN NOW() AND NOW() + INTERVAL '7 days'
        ) AS trial_por_vencer,
        COUNT(*) FILTER (WHERE
          estado = 'ACTIVO'
          AND suscripcion_fin BETWEEN NOW() AND NOW() + INTERVAL '7 days'
        ) AS suscripcion_por_vencer
      FROM licencias_chofer
    `)

    return success(res, rows[0])
  } catch (err) {
    next(err)
  }
}

/**
 * POST /api/admin/licencias/actualizar-cuota-masiva
 * Actualiza el monto_mensual de TODOS los choferes.
 * Body: { monto }
 */
export const actualizarCuotaMasiva = async (req, res, next) => {
  try {
    const { monto } = req.body
    const cuota = parseFloat(monto)

    if (isNaN(cuota) || cuota < 0) {
      return badRequest(res, 'El monto debe ser un n�mero v�lido')
    }

    const { rowCount } = await query(
      'UPDATE licencias_chofer SET monto_mensual = $1, actualizada_en = NOW()',
      [cuota]
    )

    return success(res, null, `Se ha actualizado la cuota a $${cuota} para ${rowCount} choferes.`)
  } catch (err) {
    next(err)
  }
}
