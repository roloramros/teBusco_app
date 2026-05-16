import { query, getClient } from '../config/database.js'
import { success, badRequest, notFound, forbidden } from '../utils/response.js'
import { sendNotification } from '../services/notificationService.js'
import admin from '../config/firebase.js'

export const getStats = async (req, res, next) => {
  try {
    const { provincia_id } = req.query
    let params = []
    let pClause = ''

    if (provincia_id) {
      params.push(provincia_id)
      pClause = `AND p.id = $1`
    }

    const queries = {
      total_usuarios: `
        SELECT COUNT(*) FROM usuarios u 
        LEFT JOIN municipios m ON m.id = u.municipio_id
        LEFT JOIN provincias p ON p.id = m.provincia_id
        WHERE u.tipo != 'admin' ${pClause}`,
      total_pasajeros: `
        SELECT COUNT(*) FROM usuarios u 
        LEFT JOIN municipios m ON m.id = u.municipio_id
        LEFT JOIN provincias p ON p.id = m.provincia_id
        WHERE u.tipo = 'pasajero' ${pClause}`,
      total_choferes: `
        SELECT COUNT(*) FROM choferes c 
        LEFT JOIN municipios m ON m.id = c.municipio_base_id
        LEFT JOIN provincias p ON p.id = m.provincia_id
        WHERE 1=1 ${pClause}`,
      choferes_pendientes: `
        SELECT COUNT(*) FROM choferes c 
        LEFT JOIN municipios m ON m.id = c.municipio_base_id
        LEFT JOIN provincias p ON p.id = m.provincia_id
        WHERE c.estado = 'inactivo' AND c.aprobado_en IS NULL ${pClause}`,
      choferes_activos: `
        SELECT COUNT(*) FROM choferes c 
        LEFT JOIN municipios m ON m.id = c.municipio_base_id
        LEFT JOIN provincias p ON p.id = m.provincia_id
        WHERE c.estado IN ('disponible', 'ocupado') ${pClause}`,
      viajes_completados: `
        SELECT COUNT(*) FROM solicitudes s 
        WHERE s.estado = 'completada' ${provincia_id ? 'AND s.origen_provincia_id = $1' : ''}`,
      viajes_activos: `
        SELECT COUNT(*) FROM solicitudes s 
        WHERE s.estado IN ('activa', 'en_proceso') ${provincia_id ? 'AND s.origen_provincia_id = $1' : ''}`,
      viajes_cancelados: `
        SELECT COUNT(*) FROM solicitudes s 
        WHERE s.estado = 'cancelada' ${provincia_id ? 'AND s.origen_provincia_id = $1' : ''}`,
      valoracion_promedio: `
        SELECT ROUND(AVG(v.estrellas), 2) as avg FROM valoraciones v
        JOIN choferes c ON c.id = v.chofer_id
        LEFT JOIN municipios m ON m.id = c.municipio_base_id
        LEFT JOIN provincias p ON p.id = m.provincia_id
        WHERE 1=1 ${pClause}`
    }

    const results = await Promise.all(
      Object.entries(queries).map(async ([key, sql]) => {
        const { rows } = await query(sql, params)
        const val = rows[0].count || rows[0].avg || 0
        return [key, parseFloat(val)]
      })
    )

    const stats = Object.fromEntries(results)
    return success(res, stats, 'Estadísticas obtenidas')
  } catch (err) {
    next(err)
  }
}

export const getChoferes = async (req, res, next) => {
  try {
    const { estado, pendiente, provincia_id, page = 1, limit = 20 } = req.query
    const offset = (page - 1) * limit
    const safeLimit = Math.min(limit, 50)

    let whereClauses = []
    let params = []

    if (estado) {
      whereClauses.push(`c.estado = $${params.length + 1}`)
      params.push(estado)
    }
    if (pendiente === '1' || pendiente === 'true') {
      whereClauses.push(`c.aprobado_en IS NULL`)
    }
    if (provincia_id) {
      whereClauses.push(`p.id = $${params.length + 1}`)
      params.push(provincia_id)
    }

    const whereSql = whereClauses.length > 0 ? `WHERE ${whereClauses.join(' AND ')}` : ''

    const sql = `
      SELECT c.id, c.estado, c.calificacion_promedio, c.total_viajes,
             c.opera_interprovincial, c.licencia_numero, c.aprobado_en,
             u.id as usuario_id, u.nombre, u.username, u.telefono, u.email,
             u.foto_url, u.activo, u.verificado, u.fecha_registro, u.fcm_token,
             p.nombre as provincia, m.nombre as municipio
      FROM choferes c
      JOIN usuarios u ON u.id = c.usuario_id
      LEFT JOIN municipios m ON m.id = c.municipio_base_id
      LEFT JOIN provincias p ON p.id = m.provincia_id
      ${whereSql}
      ORDER BY u.fecha_registro DESC
      LIMIT $${params.length + 1} OFFSET $${params.length + 2}
    `

    const { rows: data } = await query(sql, [...params, safeLimit, offset])
    const { rows: countRows } = await query(`SELECT COUNT(*) FROM choferes c JOIN usuarios u ON u.id = c.usuario_id LEFT JOIN municipios m ON m.id = c.municipio_base_id LEFT JOIN provincias p ON p.id = m.provincia_id ${whereSql}`, params)
    
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

export const getChoferById = async (req, res, next) => {
  try {
    const { id } = req.params

    const { rows: choferRows } = await query(`
      SELECT c.*, u.nombre, u.username, u.telefono, u.email, u.foto_url, 
             u.activo, u.verificado, u.fecha_registro, u.fcm_token,
             p.nombre as provincia, m.nombre as municipio
      FROM choferes c
      JOIN usuarios u ON u.id = c.usuario_id
      LEFT JOIN municipios m ON m.id = c.municipio_base_id
      LEFT JOIN provincias p ON p.id = m.provincia_id
      WHERE c.id = $1
    `, [id])

    if (choferRows.length === 0) return notFound(res, 'Chofer no encontrado')
    const chofer = choferRows[0]

    const [vehiculos, valoraciones, solicitudes] = await Promise.all([
      query('SELECT * FROM vehiculos WHERE chofer_id = $1', [id]),
      query(`
        SELECT v.*, u.nombre as pasajero_nombre
        FROM valoraciones v
        JOIN usuarios u ON u.id = v.pasajero_id
        WHERE v.chofer_id = $1
        ORDER BY v.creada_en DESC LIMIT 5
      `, [id]),
      query(`
        SELECT * FROM solicitudes 
        WHERE chofer_seleccionado_id = (SELECT usuario_id FROM choferes WHERE id = $1)
        AND estado = 'completada'
        ORDER BY creada_en DESC LIMIT 10
      `, [id])
    ])

    return success(res, {
      ...chofer,
      vehiculos: vehiculos.rows,
      valoraciones: valoraciones.rows,
      solicitudes_recientes: solicitudes.rows
    })
  } catch (err) {
    next(err)
  }
}

export const aprobarChofer = async (req, res, next) => {
  const client = await getClient()
  try {
    const { id } = req.params
    await client.query('BEGIN')

    const { rows: choferRows } = await client.query(
      'SELECT c.*, u.fcm_token, u.id as user_id FROM choferes c JOIN usuarios u ON u.id = c.usuario_id WHERE c.id = $1',
      [id]
    )

    if (choferRows.length === 0) {
      await client.query('ROLLBACK')
      return notFound(res, 'Chofer no encontrado')
    }

    const chofer = choferRows[0]
    if (chofer.estado !== 'inactivo') {
      await client.query('ROLLBACK')
      return badRequest(res, 'El chofer ya ha sido procesado o no está inactivo')
    }

    await client.query(
      'UPDATE choferes SET estado = $1, aprobado_por = $2, aprobado_en = NOW() WHERE id = $3',
      ['disponible', req.usuario.id, id]
    )

    await client.query('UPDATE usuarios SET verificado = true WHERE id = $1', [chofer.user_id])

    await client.query('COMMIT')

    // Notificación fuera de transacción
    sendNotification({
      usuario_id: chofer.user_id,
      tipo: 'sistema_alerta',
      titulo: '✅ ¡Cuenta aprobada!',
      cuerpo: '¡Buenas noticias! Tu cuenta de chofer ha sido aprobada. Por favor cierre sesión y vuelva a entrar para poder ofertar a solicitudes de viaje.',
      fcm_token: chofer.fcm_token
    }).catch(console.error)

    return success(res, null, 'Chofer aprobado con éxito')
  } catch (err) {
    await client.query('ROLLBACK')
    next(err)
  } finally {
    client.release()
  }
}

export const rechazarChofer = async (req, res, next) => {
  const client = await getClient()
  try {
    const { id } = req.params
    const { motivo } = req.body
    await client.query('BEGIN')

    const { rows: choferRows } = await client.query(
      'SELECT c.*, u.fcm_token, u.id as user_id FROM choferes c JOIN usuarios u ON u.id = c.usuario_id WHERE c.id = $1',
      [id]
    )

    if (choferRows.length === 0) {
      await client.query('ROLLBACK')
      return notFound(res, 'Chofer no encontrado')
    }

    const chofer = choferRows[0]

    await client.query(
      'UPDATE choferes SET estado = $1, aprobado_por = NULL, aprobado_en = NULL WHERE id = $2',
      ['inactivo', id]
    )

    await client.query('UPDATE usuarios SET verificado = false WHERE id = $1', [chofer.user_id])

    await client.query('COMMIT')

    const cuerpo = motivo 
      ? `Tu cuenta de chofer no fue aprobada. Motivo: ${motivo}`
      : 'Tu cuenta de chofer no fue aprobada. Contacta al soporte para más información.'

    sendNotification({
      usuario_id: chofer.user_id,
      tipo: 'sistema_alerta',
      titulo: '❌ Cuenta no aprobada',
      cuerpo,
      fcm_token: chofer.fcm_token
    }).catch(console.error)

    return success(res, null, 'Chofer rechazado')
  } catch (err) {
    await client.query('ROLLBACK')
    next(err)
  } finally {
    client.release()
  }
}

export const getUsuarios = async (req, res, next) => {
  try {
    const { tipo, activo, search, provincia_id, page = 1, limit = 20 } = req.query
    const offset = (page - 1) * limit
    const safeLimit = Math.min(limit, 50)

    let whereClauses = ["u.tipo != 'admin'"]
    let params = []

    if (tipo) {
      whereClauses.push(`u.tipo = $${params.length + 1}`)
      params.push(tipo)
    }
    if (activo !== undefined && activo !== '') {
      whereClauses.push(`u.activo = $${params.length + 1}`)
      params.push(activo === '1' || activo === 'true')
    }
    if (search) {
      whereClauses.push(`(u.nombre ILIKE $${params.length + 1} OR u.username ILIKE $${params.length + 1} OR u.email ILIKE $${params.length + 1} OR u.telefono ILIKE $${params.length + 1})`)
      params.push(`%${search}%`)
    }
    if (provincia_id) {
      whereClauses.push(`p.id = $${params.length + 1}`)
      params.push(provincia_id)
    }

    const whereSql = `WHERE ${whereClauses.join(' AND ')}`

const sql = `
SELECT u.id, u.nombre, u.username, u.telefono, u.email, u.tipo,
u.foto_url, u.activo, u.verificado, u.fecha_registro, u.ultimo_acceso,
p.nombre as provincia, m.nombre as municipio
FROM usuarios u
LEFT JOIN municipios m ON m.id = u.municipio_id
LEFT JOIN provincias p ON p.id = m.provincia_id
${whereSql}
ORDER BY u.fecha_registro DESC
LIMIT $${params.length + 1} OFFSET $${params.length + 2}
`

    const { rows: data } = await query(sql, [...params, safeLimit, offset])
    const { rows: countRows } = await query(`SELECT COUNT(*) FROM usuarios u LEFT JOIN municipios m ON m.id = u.municipio_id LEFT JOIN provincias p ON p.id = m.provincia_id ${whereSql}`, params)

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

export const toggleUsuarioActivo = async (req, res, next) => {
  try {
    const { id } = req.params

    const { rows: userRows } = await query('SELECT id, tipo, activo, fcm_token FROM usuarios WHERE id = $1', [id])
    if (userRows.length === 0) return notFound(res, 'Usuario no encontrado')

    const user = userRows[0]
    if (user.tipo === 'admin') return forbidden(res, 'No se puede desactivar a un administrador')

    const nuevoEstado = !user.activo
    await query('UPDATE usuarios SET activo = $1 WHERE id = $2', [nuevoEstado, id])

    // Notificación
    const titulo = nuevoEstado ? '✅ Cuenta reactivada' : '⚠️ Cuenta suspendida'
    const cuerpo = nuevoEstado
      ? 'Tu cuenta ha sido reactivada. Ya puedes usar Te Busco con normalidad.'
      : 'Tu cuenta ha sido suspendida temporalmente. Contacta al soporte.'

    sendNotification({
      usuario_id: id,
      tipo: 'sistema_alerta',
      titulo,
      cuerpo,
      fcm_token: user.fcm_token
    }).catch(console.error)

    return success(res, { activo: nuevoEstado }, `Usuario ${nuevoEstado ? 'activado' : 'desactivado'}`)
  } catch (err) {
    next(err)
  }
}

export const deleteUsuario = async (req, res, next) => {
  const client = await getClient()
  try {
    const { id } = req.params

    await client.query('BEGIN')

    const { rows: userRows } = await client.query(
      'SELECT id, tipo, nombre, fcm_token FROM usuarios WHERE id = $1',
      [id]
    )

    if (userRows.length === 0) {
      await client.query('ROLLBACK')
      return notFound(res, 'Usuario no encontrado')
    }

    const user = userRows[0]
    if (user.tipo === 'admin') {
      await client.query('ROLLBACK')
      return forbidden(res, 'No se puede eliminar un administrador')
    }

    await client.query('DELETE FROM valoraciones WHERE pasajero_id = $1', [id])
    await client.query('DELETE FROM solicitudes WHERE pasajero_id = $1', [id])
    await client.query('DELETE FROM usuarios WHERE id = $1', [id])

    await client.query('COMMIT')

    sendNotification({
      usuario_id: id,
      tipo: 'sistema_alerta',
      titulo: '❌ Cuenta eliminada',
      cuerpo: `Tu cuenta ha sido eliminada por el administrador del sistema.`,
      fcm_token: user.fcm_token
    }).catch(console.error)

    return success(res, null, 'Usuario eliminado correctamente')
  } catch (err) {
    await client.query('ROLLBACK')
    next(err)
  } finally {
    client.release()
  }
}

export const getSolicitudes = async (req, res, next) => {
  try {
    const { estado, page = 1, limit = 20 } = req.query
    const offset = (page - 1) * limit
    const safeLimit = Math.min(limit, 50)

    let whereClauses = []
    let params = []

    if (estado) {
      whereClauses.push(`s.estado = $${params.length + 1}`)
      params.push(estado)
    }

    // Si el administrador tiene un municipio asignado, filtrar por él
    if (req.usuario.municipio_id) {
      whereClauses.push(`s.origen_municipio_id = $${params.length + 1}`)
      params.push(req.usuario.municipio_id)
    }

    const whereSql = whereClauses.length > 0 ? `WHERE ${whereClauses.join(' AND ')}` : ''

    const sql = `
      SELECT v.*, uc.nombre as chofer_nombre, up.telefono as pasajero_telefono
      FROM v_solicitudes v
      JOIN solicitudes s ON s.id = v.id
      LEFT JOIN usuarios uc ON uc.id = s.chofer_seleccionado_id
      JOIN usuarios up ON up.id = s.pasajero_id
      ${whereSql}
      ORDER BY v.creada_en DESC
      LIMIT $${params.length + 1} OFFSET $${params.length + 2}
    `

    const { rows: data } = await query(sql, [...params, safeLimit, offset])
    const { rows: countRows } = await query(`SELECT COUNT(*) FROM solicitudes s ${whereSql}`, params)

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

export const broadcastNotification = async (req, res, next) => {
  try {
    const { titulo, cuerpo, tipo_usuario } = req.body

    if (!titulo || !cuerpo) return badRequest(res, 'Título y cuerpo son requeridos')

    let whereClause = "tipo != 'admin' AND fcm_token IS NOT NULL"
    let params = []

    if (tipo_usuario) {
      whereClause += " AND tipo = $1"
      params.push(tipo_usuario)
    }

    const { rows: usuarios } = await query(`SELECT id, fcm_token FROM usuarios WHERE ${whereClause}`, params)

    // ELIMINADO — reemplazado por batch + multicast
    /*
    const notifications = usuarios.map(u => 
      sendNotification({
        usuario_id: u.id,
        tipo: 'sistema_alerta',
        titulo,
        cuerpo,
        fcm_token: u.fcm_token
      })
    )

    const results = await Promise.allSettled(notifications)
    
    const enviadas = results.filter(r => r.status === 'fulfilled' && r.value !== null).length
    const fallidas = usuarios.length - enviadas
    */

    // NUEVO — INSERT batch para broadcast
    let insertadas = 0
    if (usuarios.length > 0) {
      const values = usuarios.map((_, i) =>
        `($${i * 4 + 1}, $${i * 4 + 2}, $${i * 4 + 3}, $${i * 4 + 4})`
      )
      const params = usuarios.flatMap(u => [u.id, 'sistema_alerta', titulo, cuerpo])
      await query(
        `INSERT INTO notificaciones (usuario_id, tipo, titulo, cuerpo)
         VALUES ${values.join(', ')}`,
        params
      )
      insertadas = usuarios.length
    }

    // NUEVO — FCM multicast broadcast con chunks de 500
    const FCM_CHUNK_SIZE = 500
    const tokens = usuarios.map(u => u.fcm_token).filter(t => t && t.trim() !== '')
    let enviadas = 0
    let fallidas = 0

    if (tokens.length > 0) {
      const chunks = []
      for (let i = 0; i < tokens.length; i += FCM_CHUNK_SIZE) {
        chunks.push(tokens.slice(i, i + FCM_CHUNK_SIZE))
      }
      const multicastResults = await Promise.allSettled(
        chunks.map(chunk =>
          admin.messaging().sendEachForMulticast({
            tokens: chunk,
            notification: { title: titulo, body: cuerpo },
            android: { priority: 'high', notification: { channelId: 'default_channel_id' } }
          }).catch(err => {
            console.error('❌ Error FCM broadcast chunk:', err.message)
            return { successCount: 0, failureCount: chunk.length }
          })
        )
      )
      multicastResults.forEach(r => {
        if (r.status === 'fulfilled' && r.value) {
          enviadas += r.value.successCount ?? 0
          fallidas += r.value.failureCount ?? 0
        }
      })
    }

    return success(res, { total: insertadas, enviadas, fallidas }, 'Broadcast completado')
  } catch (err) {
    next(err)
  }
}
