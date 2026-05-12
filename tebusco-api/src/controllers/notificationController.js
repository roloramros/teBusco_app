import { query } from '../config/database.js'
import { success, notFound } from '../utils/response.js'

/**
 * Obtener notificaciones del usuario autenticado
 */
export const getMyNotifications = async (req, res, next) => {
  try {
    const { id: usuarioId } = req.usuario
    const { rows } = await query(
      `SELECT * FROM notificaciones 
       WHERE usuario_id = $1 
       ORDER BY creada_en DESC 
       LIMIT 50`,
      [usuarioId]
    )
    return success(res, rows)
  } catch (err) {
    next(err)
  }
}

/**
 * Marcar una notificación como leída
 */
export const markAsRead = async (req, res, next) => {
  try {
    const { id: usuarioId } = req.usuario
    const { id: notificationId } = req.params

    const { rowCount } = await query(
      'UPDATE notificaciones SET leida = TRUE WHERE id = $1 AND usuario_id = $2',
      [notificationId, usuarioId]
    )

    if (rowCount === 0) {
      return notFound(res, 'Notificación no encontrada')
    }

    return success(res, null, 'Notificación marcada como leída')
  } catch (err) {
    next(err)
  }
}

/**
 * Marcar TODAS las notificaciones como leídas
 */
export const markAllAsRead = async (req, res, next) => {
  try {
    const { id: usuarioId } = req.usuario
    await query(
      'UPDATE notificaciones SET leida = TRUE WHERE usuario_id = $1 AND leida = FALSE',
      [usuarioId]
    )
    return success(res, null, 'Todas las notificaciones marcadas como leídas')
  } catch (err) {
    next(err)
  }
}

/**
 * Eliminar una notificación específica
 */
export const deleteNotification = async (req, res, next) => {
  try {
    const { id: usuarioId } = req.usuario
    const { id: notificationId } = req.params

    const { rowCount } = await query(
      'DELETE FROM notificaciones WHERE id = $1 AND usuario_id = $2',
      [notificationId, usuarioId]
    )

    if (rowCount === 0) {
      return notFound(res, 'Notificación no encontrada')
    }

    return success(res, null, 'Notificación eliminada correctamente')
  } catch (err) {
    next(err)
  }
}

/**
 * Eliminar TODAS las notificaciones del usuario
 */
export const deleteAllNotifications = async (req, res, next) => {
  try {
    const { id: usuarioId } = req.usuario
    await query(
      'DELETE FROM notificaciones WHERE usuario_id = $1',
      [usuarioId]
    )
    return success(res, null, 'Todas las notificaciones han sido eliminadas')
  } catch (err) {
    next(err)
  }
}
