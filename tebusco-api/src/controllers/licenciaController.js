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
        l.ultimo_pago, l.monto_mensual, l.notas,
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
 * El admin registra un pago mensual manualmente.
 * Body: { monto, meses = 1, notas }
 *
 * Lógica:
 *   - Si el chofer estaba SUSPENDIDO o TRIAL_EXPIRADO → pasa a ACTIVO
 *   - Si ya está ACTIVO → extiende suscripcion_fin N meses más
 *   - Actualiza verificado = true en usuarios
 */
export const registrarPago = async (req, res, next) => {
  const client = await getClient()
  try {
    const { chofer_id } = req.params
    const { monto = 0, meses = 1, notas } = req.body

    if (parseInt(meses) < 1 || parseInt(meses) > 12) {
      return badRequest(res, 'El número de meses debe estar entre 1 y 12')
    }

    await client.query('BEGIN')

    // Obtener licencia actual
    const { rows: licRows } = await client.query(
      `SELECT l.*, c.usuario_id, u.fcm_token, u.nombre
       FROM licencias_chofer l
       JOIN choferes c ON c.id = l.chofer_id
       JOIN usuarios u ON u.id = c.usuario_id
       WHERE l.chofer_id = $1`,
      [chofer_id]
    )

    if (licRows.length === 0) {
      await client.query('ROLLBACK')
      return notFound(res, 'Licencia no encontrada para este chofer')
    }

    const licencia = licRows[0]

    // Calcular nueva fecha de fin de suscripción
    // Si ya tiene suscripción activa y no expiró, extender desde ese fin
    // Si no, comenzar desde ahora
    const baseDate =
      licencia.estado === 'ACTIVO' && licencia.suscripcion_fin && new Date(licencia.suscripcion_fin) > new Date()
        ? licencia.suscripcion_fin
        : 'NOW()'

    const suscripcionFinSQL =
      baseDate === 'NOW()'
        ? `NOW() + INTERVAL '${parseInt(meses)} months'`
        : `$1::timestamptz + INTERVAL '${parseInt(meses)} months'`

    const updateParams =
      baseDate === 'NOW()'
        ? [monto, notas || null, chofer_id]
        : [licencia.suscripcion_fin, monto, notas || null, chofer_id]

    const suscripcionInicioSQL =
      licencia.estado === 'ACTIVO' ? 'l.suscripcion_inicio' : 'NOW()'

    await client.query(
      `UPDATE licencias_chofer l SET
        estado             = 'ACTIVO',
        suscripcion_inicio = ${suscripcionInicioSQL},
        suscripcion_fin    = ${suscripcionFinSQL},
        ultimo_pago        = NOW(),
        monto_mensual      = $${baseDate === 'NOW()' ? 1 : 2},
        notas              = $${baseDate === 'NOW()' ? 2 : 3},
        actualizada_en     = NOW()
       WHERE l.chofer_id   = $${baseDate === 'NOW()' ? 3 : 4}`,
      updateParams
    )

    // Activar verificado si estaba en false
    await client.query(
      'UPDATE usuarios SET verificado = true WHERE id = $1',
      [licencia.usuario_id]
    )

    // Actualizar estado del chofer a disponible si estaba inactivo
    await client.query(
      `UPDATE choferes SET estado = 'disponible'
       WHERE id = $1 AND estado = 'inactivo'`,
      [chofer_id]
    )

    await client.query('COMMIT')

    // Notificar al chofer
    sendNotification({
      usuario_id: licencia.usuario_id,
      tipo: 'sistema_alerta',
      titulo: '✅ Licencia activada',
      cuerpo: `Tu licencia de uso de Te Busco ha sido activada por ${meses} ${parseInt(meses) === 1 ? 'mes' : 'meses'}. ¡Ya puedes continuar operando!`,
      fcm_token: licencia.fcm_token
    }).catch(console.error)

    return success(res, null, `Pago registrado. Suscripción extendida por ${meses} ${parseInt(meses) === 1 ? 'mes' : 'meses'}`)
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

    const estadosValidos = ['TRIAL_ACTIVO', 'TRIAL_EXPIRADO', 'ACTIVO', 'SUSPENDIDO', 'BLOQUEADO']
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
        COUNT(*) FILTER (WHERE estado = 'TRIAL_EXPIRADO') AS trial_expirado,
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
