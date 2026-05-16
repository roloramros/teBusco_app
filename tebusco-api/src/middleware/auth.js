import { verifyToken, extractToken } from '../utils/jwt.js'
import { unauthorized, forbidden } from '../utils/response.js'
import { query } from '../config/database.js'

// Middleware: verifica que el request tenga un JWT válido
export const authenticate = async (req, res, next) => {
  try {
    const token = extractToken(req)
    if (!token) {
      return unauthorized(res, 'Token no proporcionado')
    }

    // Verificar firma y expiración del token
    const decoded = verifyToken(token)

    // Verificar que la sesión existe y no ha expirado en la BD
    const { rows } = await query(
      `SELECT s.id, s.expira_en,
              u.id AS usuario_id, u.nombre, u.tipo, u.activo, u.verificado,
              u.provincia_id, u.municipio_id
       FROM sesiones s
       JOIN usuarios u ON u.id = s.usuario_id
       WHERE s.token = $1 AND s.expira_en > NOW()`,
      [token]
    )

    if (rows.length === 0) {
      return unauthorized(res, 'Sesión expirada o inválida')
    }

    const sesion = rows[0]

    if (!sesion.activo) {
      return forbidden(res, 'Cuenta desactivada')
    }

    // Renovación deslizante de sesión (NUEVO)
    const SESSION_MS = parseInt(process.env.SESSION_DURATION_DAYS || '30') * 24 * 60 * 60 * 1000
    const RENEWAL_THRESHOLD_MS = SESSION_MS / 2
    const msRestantes = new Date(sesion.expira_en).getTime() - Date.now()

    if (msRestantes < RENEWAL_THRESHOLD_MS) {
      const nuevaExpiracion = new Date(Date.now() + SESSION_MS)
      query('UPDATE sesiones SET expira_en = $1 WHERE token = $2', [nuevaExpiracion, token])
        .catch(err => console.error('⚠️ Error renovando sesión:', err.message))
    }

    // Adjuntar datos del usuario al request para usarlos en controladores
    req.token = token // NUEVO
    req.usuario = {
      id:          sesion.usuario_id,
      nombre:      sesion.nombre,
      tipo:        sesion.tipo,
      verificado:  sesion.verificado,
      provincia_id: sesion.provincia_id,
      municipio_id: sesion.municipio_id,
    }

    next()
  } catch (err) {
    if (err.name === 'TokenExpiredError') {
      return unauthorized(res, 'Token expirado')
    }
    if (err.name === 'JsonWebTokenError') {
      return unauthorized(res, 'Token inválido')
    }
    next(err)
  }
}

// Middleware: solo permite acceso a ciertos tipos de usuario
export const authorize = (...tipos) => {
  return (req, res, next) => {
    if (!req.usuario) {
      return unauthorized(res)
    }
    if (!tipos.includes(req.usuario.tipo)) {
      return forbidden(res, `Acceso restringido a: ${tipos.join(', ')}`)
    }
    next()
  }
}

// Middleware: solo permite acceso si el chofer está verificado/aprobado
export const requireVerificado = (req, res, next) => {
  if (!req.usuario?.verificado) {
    return forbidden(res, 'Tu cuenta aún no ha sido verificada por un administrador')
  }
  next()
}
