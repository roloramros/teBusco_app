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

      if (users.length > 0) {
        // Crear las inserciones masivas (o individuales si preferimos simplicidad)
        for (const user of users) {
          await query(
            `INSERT INTO notificaciones (
              usuario_id, actor_id, tipo, titulo, cuerpo, datos_extra
            ) VALUES ($1, $2, $3, $4, $5, $6)`,
            [user.usuario_id, actor_id, tipo, titulo, cuerpo, JSON.stringify(datos_extra)]
          );
        }
        console.log(`📝 Notificación de tópico ${topic} registrada para ${users.length} usuarios.`);
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

      if (topic) {
        message.topic = topic
        console.log(`📤 Enviando Push a TEMA: ${topic}...`);
        admin.messaging().send(message)
          .then(res => console.log(`✅ Push enviado con éxito a tema ${topic}. Response:`, res))
          .catch(err => console.error(`❌ Error al enviar Push a tema ${topic}:`, err.message))
      } else if (fcm_token) {
        message.token = fcm_token
        console.log(`📤 Enviando Push a TOKEN individual...`);
        admin.messaging().send(message)
          .then(res => console.log(`✅ Push enviado con éxito a token. Response:`, res))
          .catch(err => console.error(`❌ Error al enviar Push a token:`, err.message))
      } else {
        console.warn('⚠️ No se proporcionó ni topic ni fcm_token. Saltando envío Push.');
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
