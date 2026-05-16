import bcrypt from 'bcryptjs'
import { query, getClient } from '../config/database.js'
import { generateToken } from '../utils/jwt.js'
import * as response from '../utils/response.js'
import { sendNotification } from '../services/notificationService.js'

const BCRYPT_ROUNDS = parseInt(process.env.BCRYPT_ROUNDS || '12')
const SESSION_MS = parseInt(process.env.SESSION_DURATION_DAYS || '30') * 24 * 60 * 60 * 1000 // NUEVO

// ---------------------------------------------------------
// POST /api/auth/registro
// ---------------------------------------------------------
export const registro = async (req, res) => {
  const client = await getClient()
  try {
    await client.query('BEGIN')

    const {
      nombre,
      username,
      telefono,
      email,
      password,
      tipo = 'pasajero',
      provincia_id,
      municipio_id,
      fcm_token
    } = req.body

    // 0. Verificar que el username no esté en uso
    const { rows: userRows } = await client.query(
      'SELECT id FROM usuarios WHERE username = $1',
      [username.trim().toLowerCase()]
    )
    if (userRows.length > 0) {
      await client.query('ROLLBACK')
      return response.conflict(res, 'El nombre de usuario ya está en uso')
    }

    // 1. Verificar que el email no esté en uso (si fue proporcionado)
    if (email) {
      const { rows: emailRows } = await client.query(
        'SELECT id FROM usuarios WHERE email = $1',
        [email.toLowerCase().trim()]
      )
      if (emailRows.length > 0) {
        await client.query('ROLLBACK')
        return response.conflict(res, 'El email ya está registrado')
      }
    }

    // 2. Verificar que el teléfono no esté en uso
    if (telefono) {
      const { rows: telRows } = await client.query(
        'SELECT id FROM usuarios WHERE telefono = $1',
        [telefono.trim()]
      )
      if (telRows.length > 0) {
        await client.query('ROLLBACK')
        return response.conflict(res, 'El teléfono ya está registrado')
      }
    }

    // 3. Validar provincia/municipio si se proporcionaron
    if (provincia_id) {
      const { rows } = await client.query(
        'SELECT id FROM provincias WHERE id = $1 AND activa = true',
        [provincia_id]
      )
      if (rows.length === 0) {
        await client.query('ROLLBACK')
        return response.badRequest(res, 'Provincia no válida')
      }
    }

    if (municipio_id && provincia_id) {
      const { rows } = await client.query(
        'SELECT id FROM municipios WHERE id = $1 AND provincia_id = $2 AND activo = true',
        [municipio_id, provincia_id]
      )
      if (rows.length === 0) {
        await client.query('ROLLBACK')
        return response.badRequest(res, 'Municipio no válido para esa provincia')
      }
    }

    // 4. Hashear la contraseña
    const password_hash = await bcrypt.hash(password, BCRYPT_ROUNDS)

    // 5. Crear el usuario
    if (fcm_token) {
      await client.query(
        'UPDATE usuarios SET fcm_token = NULL WHERE fcm_token = $1',
        [fcm_token]
      )
    }

    const { rows: newUser } = await client.query(
      `INSERT INTO usuarios
         (nombre, username, telefono, email, password_hash, tipo, provincia_id, municipio_id, fcm_token)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
       RETURNING id, nombre, username, telefono, email, tipo, activo, verificado,
                 provincia_id, municipio_id, fecha_registro`,
      [
        nombre.trim(),
        username.trim().toLowerCase(),
        telefono?.trim() || null,
        email?.toLowerCase().trim() || null,
        password_hash,
        tipo,
        provincia_id || null,
        municipio_id || null,
        fcm_token || null
      ]
    )
    const usuario = newUser[0]

    // 6. Si es chofer, crear el registro extendido en la tabla choferes
    if (tipo === 'chofer') {
      await client.query(
        `INSERT INTO choferes (usuario_id, provincia_base_id, municipio_base_id)
         VALUES ($1, $2, $3)`,
        [usuario.id, provincia_id || null, municipio_id || null]
      )
    }

    // 7. Generar token y crear sesión
    const tokenPayload = { id: usuario.id, tipo: usuario.tipo }
    const token = generateToken(tokenPayload)
    const expira = new Date(Date.now() + SESSION_MS) // MODIFICADO

    const dispositivo = (req.headers['user-agent'] || 'desconocido').substring(0, 255)

    await client.query(
      `INSERT INTO sesiones (usuario_id, token, dispositivo, expira_en)
       VALUES ($1, $2, $3, $4)`,
      [usuario.id, token, dispositivo, expira]
    )

    await client.query('COMMIT')

    // Envío de notificación de bienvenida (fuera de la transacción)
    try {
      let cuerpoNotif = 'Tu cuenta ha sido creada con éxito. ¡Bienvenido!'
      if (tipo === 'pasajero') {
        cuerpoNotif = '¡Ya eres parte de Te Busco! 🙌 Estamos comenzando, pero cada semana sumamos mejoras y funciones nuevas. Creemos que la mejor app la construyen quienes la usan — así que tu experiencia y tu retroalimentación valen oro. ¡Comparte la app y ayúdanos a crecer!'
      } else if (tipo === 'chofer') {
        cuerpoNotif = '💪 Estamos comenzando y mejorando constantemente — cada actualización nace de escuchar a choferes como tú. Tu retroalimentación construye esta app. Compártela con otros choferes y pasajeros, y entre todos hacemos que Te Busco sea lo que Cuba necesita. 🚗'
      }

      await sendNotification({
        usuario_id: usuario.id,
        actor_id: null,
        tipo: 'sistema_alerta',
        titulo: '¡Bienvenido a Te Busco! 🚕',
        cuerpo: cuerpoNotif,
        datos_extra: {},
        fcm_token: fcm_token
      })
    } catch (notifErr) {
      console.error('⚠️ Error al enviar notificación de bienvenida:', notifErr.message)
    }

    return response.created(res, {
      token,
      usuario: {
        id:          usuario.id,
        nombre:      usuario.nombre,
        username:    usuario.username,
        telefono:    usuario.telefono,
        email:       usuario.email,
        tipo:        usuario.tipo,
        verificado:  usuario.verificado,
        provincia_id: usuario.provincia_id,
        municipio_id: usuario.municipio_id,
        fecha_registro: usuario.fecha_registro,
      },
    }, 'Registro exitoso')

  } catch (err) {
    await client.query('ROLLBACK')
    throw err
  } finally {
    client.release()
  }
}

// ---------------------------------------------------------
// POST /api/auth/login
// ---------------------------------------------------------
export const login = async (req, res) => {
  const { identificador, password, fcm_token } = req.body
  // identificador puede ser email, teléfono o username

  // 1. Buscar usuario por email, teléfono o username
  const { rows } = await query(
    `SELECT id, nombre, username, telefono, email, password_hash, tipo,
            activo, verificado, provincia_id, municipio_id
     FROM usuarios
     WHERE (email = $1 OR telefono = $1 OR username = $1)
     LIMIT 1`,
    [identificador.trim().toLowerCase()]
  )

  if (rows.length === 0) {
    // Mensaje genérico para no revelar si el usuario existe
    return response.unauthorized(res, 'Credenciales incorrectas')
  }

  const usuario = rows[0]

  // 2. Verificar que la cuenta esté activa
  if (!usuario.activo) {
    return response.forbidden(res, 'Tu cuenta ha sido desactivada')
  }

  // 3. Verificar la contraseña
  const passwordValido = await bcrypt.compare(password, usuario.password_hash)
  if (!passwordValido) {
    return response.unauthorized(res, 'Credenciales incorrectas')
  }

  // 4. Generar token y registrar sesión
  const tokenPayload = { id: usuario.id, tipo: usuario.tipo }
  const token = generateToken(tokenPayload)
  const expira = new Date(Date.now() + SESSION_MS) // MODIFICADO

  const dispositivo = (req.headers['user-agent'] || 'desconocido').substring(0, 255)

  // NUEVO — Limpiar sesiones vencidas del usuario
  await query(
    'DELETE FROM sesiones WHERE usuario_id = $1 AND expira_en < NOW()',
    [usuario.id]
  )

  // NUEVO — Contar sesiones activas
  const { rows: activeSessions } = await query(
    'SELECT id FROM sesiones WHERE usuario_id = $1 ORDER BY creado_en ASC',
    [usuario.id]
  )

  // NUEVO — Límite de 3 sesiones activas simultáneas
  const MAX_SESSIONS = 3
  if (activeSessions.length >= MAX_SESSIONS) {
    const toDelete = activeSessions.slice(0, activeSessions.length - (MAX_SESSIONS - 1))
    const idsToDelete = toDelete.map(s => s.id)
    await query(
      'DELETE FROM sesiones WHERE id = ANY($1)',
      [idsToDelete]
    )
  }

  await query(
    `INSERT INTO sesiones (usuario_id, token, dispositivo, expira_en)
     VALUES ($1, $2, $3, $4)`,
    [usuario.id, token, dispositivo, expira]
  )

  // 5. Actualizar último acceso y FCM Token (si se proporciona)
  if (fcm_token) {
    // Evitar que el token esté asociado a otros usuarios (unicidad lógica por dispositivo)
    await query(
      'UPDATE usuarios SET fcm_token = NULL WHERE fcm_token = $1 AND id != $2',
      [fcm_token, usuario.id]
    )
  }

  await query(
    'UPDATE usuarios SET ultimo_acceso = NOW(), fcm_token = COALESCE($2, fcm_token) WHERE id = $1',
    [usuario.id, fcm_token || null]
  )

  // 6. Si es chofer, obtener su estado
  let choferInfo = null
  if (usuario.tipo === 'chofer') {
    const { rows: choferRows } = await query(
      `SELECT estado, calificacion_promedio, total_viajes, opera_interprovincial
       FROM choferes WHERE usuario_id = $1`,
      [usuario.id]
    )
    if (choferRows.length > 0) choferInfo = choferRows[0]
  }

  return response.success(res, {
    token,
    usuario: {
      id:          usuario.id,
      nombre:      usuario.nombre,
      username:    usuario.username,
      telefono:    usuario.telefono,
      email:       usuario.email,
      tipo:        usuario.tipo,
      verificado:  usuario.verificado,
      provincia_id: usuario.provincia_id,
      municipio_id: usuario.municipio_id,
      ...(choferInfo && { chofer: choferInfo }),
    },
  }, 'Login exitoso')
}

// ---------------------------------------------------------
// POST /api/auth/logout
// ---------------------------------------------------------
export const logout = async (req, res) => {
  const authHeader = req.headers['authorization']
  const token = authHeader?.split(' ')[1]

  if (token) {
    await query('DELETE FROM sesiones WHERE token = $1', [token])
  }

  return response.success(res, {}, 'Sesión cerrada correctamente')
}

// ---------------------------------------------------------
// GET /api/auth/me
// (requiere authenticate middleware)
// ---------------------------------------------------------
export const me = async (req, res) => {
  console.log(`--- GET /api/auth/me (ID: ${req.usuario.id}) ---`)
  try {
    const { rows } = await query(
      `SELECT u.id, u.nombre, u.username, u.telefono, u.email, u.tipo,
              u.foto_url, u.activo, u.verificado,
              u.provincia_id, u.municipio_id,
              u.fecha_registro, u.ultimo_acceso,
              p.nombre AS provincia, m.nombre AS municipio
       FROM usuarios u
       LEFT JOIN provincias p ON p.id = u.provincia_id
       LEFT JOIN municipios m ON m.id = u.municipio_id
       WHERE u.id = $1`,
      [req.usuario.id]
    )

    if (rows.length === 0) {
      console.warn(`⚠️ Usuario no encontrado: ${req.usuario.id}`)
      return response.notFound(res, 'Usuario no encontrado')
    }

    const usuario = rows[0]
    console.log(`✅ Usuario recuperado: ${usuario.username} (Provincia: ${usuario.provincia})`)
    let choferInfo = null

    if (usuario.tipo === 'chofer') {
      const { rows: choferRows = [] } = await query(
        `SELECT c.estado, c.calificacion_promedio, c.total_viajes,
                c.opera_interprovincial, c.licencia_numero,
                c.provincia_base_id, c.municipio_base_id,
                c.aprobado_en
         FROM choferes c WHERE c.usuario_id = $1`,
        [usuario.id]
      )
      if (choferRows.length > 0) choferInfo = choferRows[0]
    }

    return response.success(res, {
      ...usuario,
      ...(choferInfo && { chofer: choferInfo }),
    })
  } catch (error) {
    console.error('❌ Error en /me:', error.message)
    throw error
  }
}

// ---------------------------------------------------------
// POST /api/auth/update-fcm-token
// ---------------------------------------------------------
export const updateFcmToken = async (req, res) => {
  const { fcm_token } = req.body
  const usuarioId = req.usuario.id

  if (!fcm_token) {
    return response.badRequest(res, 'fcm_token es obligatorio')
  }

  try {
    // Evitar que el token esté asociado a otros usuarios (unicidad lógica por dispositivo)
    await query(
      'UPDATE usuarios SET fcm_token = NULL WHERE fcm_token = $1 AND id != $2',
      [fcm_token, usuarioId]
    )

    await query(
      'UPDATE usuarios SET fcm_token = $1 WHERE id = $2',
      [fcm_token, usuarioId]
    )
    return response.success(res, {}, 'Token actualizado correctamente')
  } catch (error) {
    console.error('❌ Error actualizando FCM Token:', error.message)
    throw error
  }
}

// NUEVO
export const getMisSesiones = async (req, res) => {
  try {
    const { rows } = await query(
      `SELECT id, creado_en, expira_en,
              (token = $2) AS es_sesion_actual
       FROM sesiones
       WHERE usuario_id = $1 AND expira_en > NOW()
       ORDER BY creado_en DESC`,
      [req.usuario.id, req.token]
    )
    return response.success(res, rows)
  } catch (err) {
    return response.error(res, 'Error obteniendo sesiones', 500)
  }
}

// NUEVO
export const revocarSesion = async (req, res) => {
  try {
    const { id } = req.params
    const { rows } = await query(
      'SELECT id, token FROM sesiones WHERE id = $1 AND usuario_id = $2',
      [id, req.usuario.id]
    )
    if (rows.length === 0) return response.notFound(res, 'Sesión no encontrada')
    if (rows[0].token === req.token) return response.error(res, 'No puedes revocar tu sesión actual. Usa el endpoint de logout.', 400)
    await query('DELETE FROM sesiones WHERE id = $1', [id])
    return response.success(res, null, 'Sesión revocada correctamente')
  } catch (err) {
    return response.error(res, 'Error revocando sesión', 500)
  }
}

// NUEVO
export const revocarTodasLasSesiones = async (req, res) => {
  try {
    const { rowCount } = await query(
      'DELETE FROM sesiones WHERE usuario_id = $1 AND token != $2',
      [req.usuario.id, req.token]
    )
    return response.success(res, { sesiones_cerradas: rowCount }, 'Todas las sesiones cerradas excepto la actual')
  } catch (err) {
    return response.error(res, 'Error revocando sesiones', 500)
  }
}
