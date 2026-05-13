import { query, getClient } from '../config/database.js'
import { success } from '../utils/response.js'
import { sendNotification } from '../services/notificationService.js'

export const getStats = async (req, res, next) => {
  try {
    const queries = {
      total_usuarios: "SELECT COUNT(*) FROM usuarios WHERE tipo != 'admin'",
      total_pasajeros: "SELECT COUNT(*) FROM usuarios WHERE tipo = 'pasajero'",
      total_choferes: "SELECT COUNT(*) FROM choferes",
      choferes_pendientes: "SELECT COUNT(*) FROM choferes WHERE estado = 'inactivo' AND aprobado_en IS NULL",
      choferes_activos: "SELECT COUNT(*) FROM choferes WHERE estado IN ('disponible', 'ocupado')",
      viajes_completados: "SELECT COUNT(*) FROM solicitudes WHERE estado = 'completada'",
      viajes_activos: "SELECT COUNT(*) FROM solicitudes WHERE estado IN ('activa', 'en_proceso')",
      viajes_cancelados: "SELECT COUNT(*) FROM solicitudes WHERE estado = 'cancelada'",
      valoracion_promedio: "SELECT ROUND(AVG(estrellas), 2) as avg FROM valoraciones"
    }

    const results = await Promise.all(
      Object.entries(queries).map(async ([key, sql]) => {
        const { rows } = await query(sql)
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
    const { estado, pendiente, page = 1, limit = 20 } = req.query
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
    const { rows: countRows } = await query(`SELECT COUNT(*) FROM choferes c ${whereSql}`, params)
    
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

    if (choferRows.length === 0) return res.status(404).json({ ok: false, message: 'Chofer no encontrado' })
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
      return res.status(404).json({ ok: false, message: 'Chofer no encontrado' })
    }

    const chofer = choferRows[0]
    if (chofer.estado !== 'inactivo') {
      await client.query('ROLLBACK')
      return res.status(400).json({ ok: false, message: 'El chofer ya ha sido procesado o no está inactivo' })
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
      cuerpo: '¡Buenas noticias! Tu cuenta de chofer ha sido aprobada. Ya puedes recibir solicitudes de viaje.',
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
export const getUsuarios = async (req, res, next) => {
  try {
    const { tipo, activo, search, page = 1, limit = 20 } = req.query;
    const offset = (page - 1) * limit;
    const safeLimit = Math.min(limit, 50);

    let whereClauses = ["u.tipo != 'admin'"];
    let params = [];

    if (tipo) {
      whereClauses.push(`u.tipo = $${params.length + 1}`);
      params.push(tipo);
    }
    if (activo !== undefined) {
      whereClauses.push(`u.activo = $${params.length + 1}`);
      params.push(activo === '1' || activo === 'true');
    }
    if (search) {
      whereClauses.push(`(u.nombre ILIKE $${params.length + 1} OR u.username ILIKE $${params.length + 1} OR u.email ILIKE $${params.length + 1} OR u.telefono ILIKE $${params.length + 1})`);
      params.push(`%${search}%`);
    }

    const whereSql = `WHERE ${whereClauses.join(' AND ')}`;

    const sql = `
      SELECT u.id, u.nombre, u.username, u.telefono, u.email, u.tipo, 
             u.foto_url, u.activo, u.verificado, u.fecha_registro,
             p.nombre as provincia, m.nombre as municipio
      FROM usuarios u
      LEFT JOIN municipios m ON m.id = u.municipio_id
      LEFT JOIN provincias p ON p.id = m.provincia_id
      ${whereSql}
      ORDER BY u.fecha_registro DESC
      LIMIT $${params.length + 1} OFFSET $${params.length + 2}
    `;

    const { rows: data } = await query(sql, [...params, safeLimit, offset]);
    const { rows: countRows } = await query(`SELECT COUNT(*) FROM usuarios u ${whereSql}`, params);

    return success(res, {
      data,
      total: parseInt(countRows[0].count),
      page: parseInt(page),
      limit: safeLimit
    });
  } catch (err) {
    next(err);
  }
};

export const toggleUsuarioActivo = async (req, res, next) => {
  try {
    const { id } = req.params;

    const { rows: userRows } = await query('SELECT id, tipo, activo, fcm_token FROM usuarios WHERE id = $1', [id]);
    if (userRows.length === 0) return res.status(404).json({ ok: false, message: 'Usuario no encontrado' });

    const user = userRows[0];
    if (user.tipo === 'admin') return res.status(403).json({ ok: false, message: 'No se puede desactivar a un administrador' });

    const nuevoEstado = !user.activo;
    await query('UPDATE usuarios SET activo = $1 WHERE id = $2', [nuevoEstado, id]);

    // Notificación
    const titulo = nuevoEstado ? '✅ Cuenta reactivada' : '⚠️ Cuenta suspendida';
    const cuerpo = nuevoEstado 
      ? 'Tu cuenta ha sido reactivada. Ya puedes usar Te Busco con normalidad.'
      : 'Tu cuenta ha sido suspendida temporalmente. Contacta al soporte.';

    sendNotification({
      usuario_id: id,
      tipo: 'sistema_alerta',
      titulo,
      cuerpo,
      fcm_token: user.fcm_token
    }).catch(console.error);

    return success(res, { activo: nuevoEstado }, `Usuario ${nuevoEstado ? 'activado' : 'desactivado'}`)
  } catch (err) {
    next(err)
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
      whereClauses.push(`s.estado = $1`)
      params.push(estado)
    }

    const whereSql = whereClauses.length > 0 ? `WHERE ${whereClauses.join(' AND ')}` : ''

    const sql = `
      SELECT s.*, uc.nombre as chofer_nombre
      FROM v_solicitudes s
      LEFT JOIN usuarios uc ON uc.id = s.chofer_seleccionado_id
      ${whereSql}
      ORDER BY s.creada_en DESC
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