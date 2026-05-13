import { query } from '../config/database.js'
import { success } from '../utils/response.js'

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
