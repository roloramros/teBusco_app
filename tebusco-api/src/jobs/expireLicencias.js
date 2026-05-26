import cron from 'node-cron'
import { query, getClient } from '../config/database.js'
import { sendNotification } from '../services/notificationService.js'

export function startExpireLicenciasJob() {
  console.log('🪪  Job de expiración y renovación de licencias iniciado.')

  // Ejecutar una vez al día a las 2:00 AM
  cron.schedule('05 04 * * *', async () => {
    console.log('🔄 [LicenciaJob] Revisando licencias y renovaciones automáticas...')
    const client = await getClient()
    try {
      await client.query('BEGIN')

      // 1. Encontrar todas las licencias vencidas (TRIAL o ACTIVO)
      const { rows: porVencer } = await client.query(`
        SELECT l.*, c.usuario_id, u.fcm_token
        FROM licencias_chofer l
        JOIN choferes c ON c.id = l.chofer_id
        JOIN usuarios u ON u.id = c.usuario_id
        WHERE (l.estado = 'TRIAL_ACTIVO' AND l.trial_fin < NOW())
           OR (l.estado = 'ACTIVO' AND l.suscripcion_fin < NOW())
        FOR UPDATE
      `)

      let renovados = 0
      let suspendidos = 0
      let trialsExpirados = 0

      for (const licencia of porVencer) {
        const cuota = parseFloat(licencia.monto_mensual || 0)
        const saldo = parseFloat(licencia.saldo_fondo || 0)

        if (saldo >= cuota && cuota > 0) {
          // TIENE SALDO: Renovación automática
          const nuevoSaldo = saldo - cuota
          const hoy = new Date()
          const nuevaFechaFin = new Date(hoy.setMonth(hoy.getMonth() + 1))
          
          await client.query(`
            UPDATE licencias_chofer SET
              estado = 'ACTIVO',
              saldo_fondo = $1,
              suscripcion_inicio = NOW(),
              suscripcion_fin = $2,
              actualizada_en = NOW()
            WHERE chofer_id = $3
          `, [nuevoSaldo, nuevaFechaFin, licencia.chofer_id])
          
          renovados++

          // Notificar renovación
          sendNotification({
            usuario_id: licencia.usuario_id,
            tipo: 'sistema_alerta',
            titulo: '✅ Renovación Automática',
            cuerpo: `Se descontaron $${cuota.toFixed(2)} de tu fondo. Tu licencia se renovó por 1 mes. Saldo restante: $${nuevoSaldo.toFixed(2)}`,
            fcm_token: licencia.fcm_token
          }).catch(console.error)
        } else {
          // NO TIENE SALDO: Suspender solo la licencia
          const nuevoEstado = 'SUSPENDIDO'
          await client.query(`
            UPDATE licencias_chofer SET
              estado = $1,
              actualizada_en = NOW()
            WHERE chofer_id = $2
          `, [nuevoEstado, licencia.chofer_id])

          // El chofer permanece verificado y disponible según la instrucción del usuario
          suspendidos++

          // Notificar suspensión de licencia
          sendNotification({
            usuario_id: licencia.usuario_id,
            tipo: 'sistema_alerta',
            titulo: '⚠️ Licencia vencida',
            cuerpo: 'Tu licencia ha vencido y no tienes saldo suficiente para la renovación automática. Recarga tu monedero para regularizar tu situación.',
            fcm_token: licencia.fcm_token
          }).catch(console.error)
        }
      }

      await client.query('COMMIT')

      // 2. Alerta preventiva: notificar 7 días antes del vencimiento
      const { rows: alertasVencimiento } = await query(`
        SELECT u.id AS usuario_id, u.fcm_token, l.estado, l.saldo_fondo, l.monto_mensual,
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

      await Promise.allSettled(alertasVencimiento.map(chofer => {
        const saldo = parseFloat(chofer.saldo_fondo || 0)
        const cuota = parseFloat(chofer.monto_mensual || 0)
        const tipo = chofer.estado === 'TRIAL_ACTIVO' ? 'período de prueba' : 'suscripción'
        
        // Mensaje dinámico si tiene o no fondos para la auto-renovación
        let cuerpo = `Te quedan ${chofer.dias_restantes} días de ${tipo}. `
        if (saldo >= cuota && cuota > 0) {
          cuerpo += `Se descontarán $${cuota.toFixed(2)} automáticamente de tu fondo para renovar.`
        } else {
          cuerpo += `No tienes saldo suficiente. Recarga tu monedero pronto para evitar interrupciones.`
        }

        return sendNotification({
          usuario_id: chofer.usuario_id,
          tipo: 'sistema_alerta',
          titulo: '📅 Tu licencia vence pronto',
          cuerpo: cuerpo,
          fcm_token: chofer.fcm_token
        })
      }))

      console.log(`✅ [LicenciaJob] Renovados: ${renovados} | Trials expirados: ${trialsExpirados} | Suspendidos: ${suspendidos} | Alertas enviadas: ${alertasVencimiento.length}`)
    } catch (err) {
      await client.query('ROLLBACK')
      console.error(`❌ [LicenciaJob] Error: ${err.message}`)
    } finally {
      client.release()
    }
  })
}
