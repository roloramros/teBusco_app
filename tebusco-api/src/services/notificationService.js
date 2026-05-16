import { query } from '../config/database.js'
import admin from '../config/firebase.js'

/**
 * Servicio centralizado para notificaciones (Persistencia + Push)
 */
export const sendNotification = async ({
  usuario_id,
  actor_id = null,
  tipo,
  titulo,
  cuerpo,
  datos_extra = {},
  fcm_token = null,
  topic = null
}) => {
  try {
    let lastNotifId = null;

    // 1. Persistencia en Base de Datos
    if (topic && topic.startsWith('municipio_')) {
      // Si es un tópico de municipio, persistir para TODOS los choferes de ese municipio
      const municipioId = topic.replace('municipio_', '');
      
      // Obtener todos los IDs de usuario que son choferes en ese municipio
      const { rows: users } = await query(
        `SELECT usuario_id FROM choferes WHERE municipio_base_id = $1`,
        [municipioId]
      );

      // NUEVO — INSERT batch: una sola query para todos los usuarios
      if (users.length > 0) {
        const values = users.map((_, i) =>
          `($${i * 6 + 1}, $${i * 6 + 2}, $${i * 6 + 3}, $${i * 6 + 4}, $${i * 6 + 5}, $${i * 6 + 6})`
        )
        const params = users.flatMap(u => [
          u.usuario_id,
          actor_id ?? null,
          tipo,
          titulo,
          cuerpo,
          JSON.stringify(datos_extra)
        ])
        await query(
          `INSERT INTO notificaciones (usuario_id, actor_id, tipo, titulo, cuerpo, datos_extra)
           VALUES ${values.join(', ')}`,
          params
        )
        console.log(`📝 Notificación de tópico ${topic} registrada para ${users.length} usuarios en batch.`)
      }
    } else if (usuario_id) {
      // Persistencia normal para un usuario específico
      const { rows } = await query(
        `INSERT INTO notificaciones (
          usuario_id, actor_id, tipo, titulo, cuerpo, datos_extra
        ) VALUES ($1, $2, $3, $4, $5, $6)
        RETURNING *`,
        [usuario_id, actor_id, tipo, titulo, cuerpo, JSON.stringify(datos_extra)]
      )
      lastNotifId = rows[0].id;
      console.log(`📝 Notificación registrada en BD (ID: ${lastNotifId}, Tipo: ${tipo})`)
    }

      // 2. Enviar por FCM (Push) si se proporciona token o tema
    console.log(`📡 Preparando envío Push: topic=${topic}, token=${fcm_token ? 'SÍ' : 'NO'}, firebase_init=${admin.apps.length > 0}`);
    
    if (admin.apps.length > 0) {
      // Normalizar datos_extra: Firebase requiere que todos los valores en 'data' sean STRINGS
      const normalizedData = {}
      if (datos_extra) {
        Object.keys(datos_extra).forEach(key => {
          normalizedData[key] = String(datos_extra[key])
        })
      }

      const message = {
        notification: {
          title: titulo,
          body: cuerpo
        },
        android: {
          priority: 'high',
          notification: {
            channelId: 'default_channel_id',
            icon: 'ic_notifications',
            color: '#0055FF'
          }
        },
        data: {
          ...normalizedData,
          tipo: String(tipo)
        }
      }

      // Si tenemos un ID de notificación (envío individual), incluirlo
      if (lastNotifId) {
        message.data.notificacion_id = String(lastNotifId);
      }

      console.log(`📦 Payload FCM: ${JSON.stringify(message.notification)} | Data: ${JSON.stringify(message.data)}`);

      if (topic) {
        // NUEVO — FCM multicast con chunks de 500 (límite Firebase)
        try {
          if (topic.startsWith('municipio_')) {
            const municipioId = topic.replace('municipio_', '');
            const FCM_CHUNK_SIZE = 500
            const { rows: usersWithTokens } = await query(
              `SELECT u.fcm_token FROM choferes c
               JOIN usuarios u ON u.id = c.usuario_id
               WHERE c.municipio_base_id = $1 AND u.fcm_token IS NOT NULL AND u.fcm_token != ''`,
              [municipioId]
            )
            const tokens = usersWithTokens.map(u => u.fcm_token)

            if (tokens.length > 0) {
              const chunks = []
              for (let i = 0; i < tokens.length; i += FCM_CHUNK_SIZE) {
                chunks.push(tokens.slice(i, i + FCM_CHUNK_SIZE))
              }
              const multicastMessage = {
                notification: { title: titulo, body: cuerpo },
                android: message.android,
                data: message.data
              }
              await Promise.allSettled(
                chunks.map(chunk =>
                  admin.messaging().sendEachForMulticast({ ...multicastMessage, tokens: chunk })
                    .then(r => console.log(`✅ FCM multicast: ${r.successCount} enviados, ${r.failureCount} fallidos`))
                    .catch(err => console.error('❌ Error FCM multicast municipio:', err.message))
                )
              )
            }
          } else {
            // Soporte para otros tópicos no municipio_ (envío normal)
            message.topic = topic
            // MODIFICADO — Añadido await
            await admin.messaging().send(message)
              .then(res => console.log(`✅ Push enviado con éxito a tema ${topic}. Response:`, res))
              .catch(err => console.error(`❌ Error al enviar Push a tema ${topic}:`, err.message))
          }
        } catch (fcmErr) {
          console.error('❌ Error en Fase 2 (FCM Topic/Multicast):', fcmErr.message);
        }
      } else if (fcm_token && fcm_token.trim() !== '') {
        // MODIFICADO — Validación de token vacío y añadido await
        message.token = fcm_token
        console.log(`📤 Enviando Push a TOKEN individual: ${fcm_token.substring(0, 10)}...`);
        try {
          const res = await admin.messaging().send(message);
          console.log(`✅ Push enviado con éxito a token. Response:`, res);
        } catch (err) {
          console.error(`❌ Error al enviar Push a token:`, err.message);
        }
      } else {
        console.warn(`⚠️ No se proporcionó ni topic ni fcm_token válido para usuario_id: ${usuario_id}. Saltando envío Push.`);
      }
    } else {
      console.error('❌ Firebase Admin no está inicializado. No se puede enviar Push.');
    }

    return true
  } catch (err) {
    console.error('❌ Error en sendNotification:', err.message)
    // No lanzamos el error para no romper el flujo principal si falla la notificación
    return null
  }
}
