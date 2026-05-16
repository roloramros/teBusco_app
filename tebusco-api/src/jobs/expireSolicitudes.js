import cron from 'node-cron'
import { query } from '../config/database.js'
import { sendNotification } from '../services/notificationService.js'

export function startExpireSolicitudesJob() {
  console.log('🕐 Job de expiración de solicitudes iniciado.')

  cron.schedule('*/30 * * * *', async () => {
    console.log('🔄 [ExpireJob] Revisando solicitudes expiradas...')
    try {
      const updateQuery = `
        UPDATE solicitudes
        SET estado = 'expirada'
        WHERE estado = 'activa'
          AND (
            (es_inmediato = true  AND creada_en  < NOW() - INTERVAL '3 hours')
            OR
            (es_inmediato = false AND fecha_viaje < NOW() - INTERVAL '24 hours')
          )
        RETURNING id, pasajero_id
      `
      const { rows: expiredRequests } = await query(updateQuery)

      if (expiredRequests.length > 0) {
        const notificationPromises = expiredRequests.map(async (fila) => {
          try {
            // Obtener el fcm_token del pasajero
            const { rows: userRows } = await query(
              'SELECT fcm_token FROM usuarios WHERE id = $1',
              [fila.pasajero_id]
            )
            const fcmToken = userRows.length > 0 ? userRows[0].fcm_token : null

            return sendNotification({
              usuario_id: fila.pasajero_id,
              actor_id: null,
              tipo: 'sistema_alerta',
              titulo: 'Tu solicitud ha expirado',
              cuerpo: 'Tu solicitud de viaje no recibió respuesta a tiempo y fue cerrada automáticamente. Puedes crear una nueva cuando quieras.',
              datos_extra: { solicitud_id: fila.id },
              fcm_token: fcmToken
            })
          } catch (err) {
            console.error(`❌ [ExpireJob] Error procesando notificación para solicitud ${fila.id}:`, err.message)
          }
        })

        await Promise.allSettled(notificationPromises)
      }

      console.log(`✅ [ExpireJob] ${expiredRequests.length} solicitudes expiradas.`)
    } catch (error) {
      console.error(`❌ [ExpireJob] Error: ${error.message}`)
    }
  })
}
