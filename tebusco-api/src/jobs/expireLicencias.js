import cron from 'node-cron'
import { query } from '../config/database.js'
import { sendNotification } from '../services/notificationService.js'

export function startExpireLicenciasJob() {
  console.log('🪪  Job de expiración de licencias iniciado.')

  // Ejecutar una vez al día a las 2:00 AM
  cron.schedule('0 2 * * *', async () => {
    console.log('🔄 [LicenciaJob] Revisando licencias expiradas...')
    try {
      // 1. Expirar trials vencidos
      const { rows: trialsExpirados } = await query(`
        UPDATE licencias_chofer SET
          estado = 'TRIAL_EXPIRADO',
          actualizada_en = NOW()
        WHERE estado = 'TRIAL_ACTIVO'
          AND trial_fin < NOW()
        RETURNING chofer_id
      `)

      // 2. Suspender suscripciones vencidas
      const { rows: suscripcionesExpiradas } = await query(`
        UPDATE licencias_chofer SET
          estado = 'SUSPENDIDO',
          actualizada_en = NOW()
        WHERE estado = 'ACTIVO'
          AND suscripcion_fin < NOW()
        RETURNING chofer_id
      `)

      const choferIdsAfectados = [
        ...trialsExpirados.map(r => r.chofer_id),
        ...suscripcionesExpiradas.map(r => r.chofer_id)
      ]

      if (choferIdsAfectados.length > 0) {
        // 3. Desactivar verificado y poner choferes como inactivos
        await query(`
          UPDATE usuarios u SET verificado = false
          FROM choferes c
          WHERE c.usuario_id = u.id
            AND c.id = ANY($1)
        `, [choferIdsAfectados])

        await query(`
          UPDATE choferes SET estado = 'inactivo'
          WHERE id = ANY($1)
        `, [choferIdsAfectados])

        // 4. Notificar a cada chofer afectado
        const { rows: afectados } = await query(`
          SELECT u.id AS usuario_id, u.fcm_token, l.estado
          FROM licencias_chofer l
          JOIN choferes c ON c.id = l.chofer_id
          JOIN usuarios u ON u.id = c.usuario_id
          WHERE c.id = ANY($1)
        `, [choferIdsAfectados])

        await Promise.allSettled(afectados.map(chofer => {
          const esTrial = chofer.estado === 'TRIAL_EXPIRADO'
          return sendNotification({
            usuario_id: chofer.usuario_id,
            tipo: 'sistema_alerta',
            titulo: esTrial ? '⏰ Tu período de prueba ha terminado' : '⚠️ Tu suscripción ha vencido',
            cuerpo: esTrial
              ? 'Tu trial gratuito de 45 días ha finalizado. Contacta al administrador para activar tu licencia y seguir operando en Te Busco.'
              : 'Tu suscripción mensual ha vencido. Renueva tu licencia para volver a recibir solicitudes de viaje.',
            fcm_token: chofer.fcm_token
          })
        }))
      }

      // 5. Alerta preventiva: notificar 7 días antes del vencimiento
      const { rows: porVencer } = await query(`
        SELECT u.id AS usuario_id, u.fcm_token, l.estado,
               CASE
                 WHEN l.estado = 'TRIAL_ACTIVO'
                   THEN EXTRACT(DAY FROM l.trial_fin - NOW())::int
                 ELSE EXTRACT(DAY FROM l.suscripcion_fin - NOW())::int
               END AS dias_restantes
        FROM licencias_chofer l
        JOIN choferes c ON c.id = l.chofer_id
        JOIN usuarios u ON u.id = c.usuario_id
        WHERE (
          (l.estado = 'TRIAL_ACTIVO'
            AND l.trial_fin BETWEEN NOW() + INTERVAL '6 days' AND NOW() + INTERVAL '7 days')
          OR
          (l.estado = 'ACTIVO'
            AND l.suscripcion_fin BETWEEN NOW() + INTERVAL '6 days' AND NOW() + INTERVAL '7 days')
        )
      `)

      await Promise.allSettled(porVencer.map(chofer =>
        sendNotification({
          usuario_id: chofer.usuario_id,
          tipo: 'sistema_alerta',
          titulo: '📅 Tu licencia vence pronto',
          cuerpo: `Te quedan ${chofer.dias_restantes} días de ${chofer.estado === 'TRIAL_ACTIVO' ? 'período de prueba' : 'suscripción'}. Contacta al administrador para renovar y seguir operando sin interrupciones.`,
          fcm_token: chofer.fcm_token
        })
      ))

      console.log(`✅ [LicenciaJob] Trials expirados: ${trialsExpirados.length} | Suscripciones vencidas: ${suscripcionesExpiradas.length} | Alertas enviadas: ${porVencer.length}`)
    } catch (err) {
      console.error(`❌ [LicenciaJob] Error: ${err.message}`)
    }
  })
}
