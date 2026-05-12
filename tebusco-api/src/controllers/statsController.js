import { query } from '../config/database.js'
import { success } from '../utils/response.js'

/**
 * Obtener estadísticas públicas de la plataforma
 * No requiere autenticación
 */
export const getPublicStats = async (req, res, next) => {
  try {
    const queries = {
      total_usuarios: "SELECT COUNT(*)::int FROM usuarios WHERE tipo != 'admin'",
      total_choferes: "SELECT COUNT(*)::int FROM choferes",
      viajes_completados: "SELECT COUNT(*)::int FROM solicitudes WHERE estado = 'completada'",
      viajes_activos: "SELECT COUNT(*)::int FROM solicitudes WHERE estado IN ('activa', 'en_proceso')"
    };

    const results = {};
    for (const [key, sql] of Object.entries(queries)) {
      const { rows } = await query(sql);
      results[key] = rows[0].count;
    }

    return success(res, results);
  } catch (err) {
    console.error('❌ Error en getPublicStats:', err.message);
    next(err);
  }
}
