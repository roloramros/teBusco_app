import { query, getClient } from '../config/database.js'
import { success, created, badRequest, notFound, forbidden } from '../utils/response.js'
import admin from '../config/firebase.js'
import { sendNotification } from '../services/notificationService.js'

/**
 * Crear una nueva solicitud de viaje (Pasajero)
 */
export const createSolicitud = async (req, res, next) => {
  try {
    const { id: pasajeroId, nombre: pasajeroNombre } = req.usuario
    let {
      origen_descripcion, origen_lat, origen_lng,
      destino_descripcion, destino_lat, destino_lng,
      origen_municipio_id,
      origen_provincia_nombre, origen_municipio_nombre,
      destino_provincia_nombre, destino_municipio_nombre,
      paradas, // Array JSONB
      precio_oferta, moneda, num_pasajeros, tipo_carga, descripcion,
      es_inmediato, fecha_viaje, distancia
    } = req.body

    // Validaciones mínimas
    if (!origen_descripcion || !destino_descripcion) {
      return badRequest(res, 'Origen y destino son obligatorios')
    }

    // Resolver IDs de Provincia/Municipio si se enviaron nombres
    let resolved_origen_provincia_id = null;
    let resolved_origen_municipio_id = origen_municipio_id || null;
    let resolved_destino_provincia_id = null;
    let resolved_destino_municipio_id = null;

    // Helper para resolver municipio y su provincia
    const resolverUbicacion = async (provNombre, munNombre) => {
      if (!munNombre) return { pId: null, mId: null };

      // Intentar encontrar el municipio por nombre (ignorando mayúsculas/minúsculas)
      // Si se da la provincia, filtramos por ella para evitar ambigüedades
      let queryMun = 'SELECT m.id, m.provincia_id FROM municipios m ';
      let paramsMun = [munNombre.trim()];

      if (provNombre) {
        queryMun += 'JOIN provincias p ON p.id = m.provincia_id WHERE m.nombre ILIKE $1 AND p.nombre ILIKE $2 LIMIT 1';
        paramsMun.push(provNombre.trim());
      } else {
        queryMun += 'WHERE m.nombre ILIKE $1 LIMIT 1';
      }

      const resMun = await query(queryMun, paramsMun);
      if (resMun.rows.length > 0) {
        return { pId: resMun.rows[0].provincia_id, mId: resMun.rows[0].id };
      }
      return { pId: null, mId: null };
    };

    if (origen_municipio_nombre) {
      const { pId, mId } = await resolverUbicacion(origen_provincia_nombre, origen_municipio_nombre);
      resolved_origen_provincia_id = pId;
      resolved_origen_municipio_id = mId || resolved_origen_municipio_id;
    }

    if (destino_municipio_nombre) {
      const { pId, mId } = await resolverUbicacion(destino_provincia_nombre, destino_municipio_nombre);
      resolved_destino_provincia_id = pId;
      resolved_destino_municipio_id = mId;
    }

    // Forzar precio a 0 para nuevas solicitudes (basado en el nuevo flujo de ofertas)
    precio_oferta = 0;

    // Normalizar moneda: debe ser un arreglo de strings en mayúsculas
    if (!Array.isArray(moneda) || moneda.length === 0) {
      moneda = ['CUP'];
    } else {
      moneda = moneda.map(m => String(m).trim().toUpperCase());
    }

    const { rows } = await query(
      `INSERT INTO solicitudes (
        pasajero_id, 
        origen_descripcion, origen_lat, origen_lng,
        destino_descripcion, destino_lat, destino_lng,
        origen_provincia_id, origen_municipio_id,
        destino_provincia_id, destino_municipio_id,
        paradas,
        precio_oferta, moneda, num_pasajeros, tipo_carga, descripcion,
        es_inmediato, fecha_viaje, distancia
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14::text[], $15, $16, $17, $18, $19, $20)
      RETURNING *`,
      [
        pasajeroId,
        origen_descripcion, origen_lat, origen_lng,
        destino_descripcion, destino_lat, destino_lng,
        resolved_origen_provincia_id, resolved_origen_municipio_id,
        resolved_destino_provincia_id, resolved_destino_municipio_id,
        JSON.stringify(paradas || []),
        precio_oferta,
        moneda,
        num_pasajeros || 1,
        tipo_carga || null,
        descripcion,
        es_inmediato !== undefined ? es_inmediato : true,
        fecha_viaje || null,
        distancia || 0
      ]
    )

    const nuevaSolicitud = rows[0]

    // ─────────────────────────────────────────────────────────
    // NOTIFICACIÓN (Choferes del municipio)
    // ─────────────────────────────────────────────────────────
    if (process.env.NODE_ENV !== 'development') {
      const target_municipio_id = resolved_origen_municipio_id;
      if (target_municipio_id) {
        await sendNotification({
          usuario_id: null, // No hay un receptor único, es por tema
          tipo: 'nueva_solicitud',
          titulo: '🚕 ¡Nueva solicitud de viaje!',
          cuerpo: `${pasajeroNombre} busca viaje desde ${origen_descripcion} hasta ${destino_descripcion}`,
          datos_extra: { solicitud_id: nuevaSolicitud.id.toString() },
          topic: `municipio_${target_municipio_id}`
        });
      }
    }

    return created(res, nuevaSolicitud, '¡Solicitud publicada con éxito!')
  } catch (err) {
    next(err)
  }
}

/**
 * Obtener TODAS las solicitudes activas (Radar para el Chofer)
 */
export const getTodasSolicitudesActivas = async (req, res, next) => {
  try {
    const { id: usuarioId } = req.usuario;

    // 1. Obtener el ID de chofer del usuario actual
    const { rows: choferRows } = await query(
      'SELECT id FROM choferes WHERE usuario_id = $1',
      [usuarioId]
    )
    const choferId = choferRows.length > 0 ? choferRows[0].id : null;

    const { rows } = await query(
      `SELECT v.*, 
        CASE WHEN r.id IS NOT NULL THEN TRUE ELSE FALSE END as ha_respondido
       FROM v_solicitudes v
       LEFT JOIN respuestas_solicitud r ON r.solicitud_id = v.id AND r.chofer_id = $1 AND r.estado != 'rechazado'
       WHERE v.estado = 'activa' 
       ORDER BY v.creada_en DESC`,
      [choferId]
    )
    return success(res, rows)
  } catch (err) {
    next(err)
  }
}

/**
 * Obtener el historial de solicitudes del pasajero autenticado
 */
export const getMisSolicitudes = async (req, res, next) => {
  try {
    const { id: pasajeroId } = req.usuario
    const { rows } = await query(
      'SELECT * FROM v_solicitudes WHERE pasajero_id = $1 ORDER BY creada_en DESC',
      [pasajeroId]
    )
    return success(res, rows)
  } catch (err) {
    next(err)
  }
}

/**
 * Obtener detalles de una solicitud específica
 */
export const getSolicitudById = async (req, res, next) => {
  try {
    const { id } = req.params
    const { rows } = await query(
      'SELECT * FROM v_solicitudes WHERE id = $1',
      [id]
    )

    if (rows.length === 0) {
      return notFound(res, 'Solicitud no encontrada')
    }

    return success(res, rows[0])
  } catch (err) {
    next(err)
  }
}

/**
 * Responder a una solicitud (Chofer hace una oferta)
 */
export const responderSolicitud = async (req, res, next) => {
  try {
    const { id: usuarioId } = req.usuario
    const { solicitud_id } = req.params
    const { vehiculo_id, mensaje, precio_propuesto, moneda, tiempo_arribo_min } = req.body

    // 1. Obtener el ID de chofer del usuario actual
    const { rows: choferRows } = await query(
      'SELECT id FROM choferes WHERE usuario_id = $1',
      [usuarioId]
    )

    if (choferRows.length === 0) {
      return badRequest(res, 'Solo los choferes pueden responder a solicitudes')
    }
    const choferId = choferRows[0].id

    // 2. Verificar que la solicitud existe y está activa
    const { rows: solRows } = await query(
      'SELECT pasajero_id FROM solicitudes WHERE id = $1 AND estado = $2',
      [solicitud_id, 'activa']
    )

    if (solRows.length === 0) {
      return notFound(res, 'La solicitud no existe o ya no está activa')
    }
    const { pasajero_id: pasajeroId } = solRows[0]

    // 3. Crear la respuesta/oferta
    const { rows: respRows } = await query(
      `INSERT INTO respuestas_solicitud (
        solicitud_id, chofer_id, vehiculo_id, mensaje, precio_propuesto, moneda, tiempo_arribo_min
      ) VALUES ($1, $2, $3, $4, $5, $6, $7)
      ON CONFLICT (solicitud_id, chofer_id) DO UPDATE SET
        vehiculo_id = EXCLUDED.vehiculo_id,
        mensaje = EXCLUDED.mensaje,
        precio_propuesto = EXCLUDED.precio_propuesto,
        moneda = EXCLUDED.moneda,
        tiempo_arribo_min = EXCLUDED.tiempo_arribo_min,
        estado = 'pendiente',
        respondido_en = NOW()
      RETURNING *`,
      [solicitud_id, choferId, vehiculo_id, mensaje, precio_propuesto, moneda || 'CUP', tiempo_arribo_min]
    )

    const nuevaOferta = respRows[0]

    // 4. Notificar al pasajero (Persistencia + Push)
    try {
      const { rows: userRows } = await query(
        'SELECT fcm_token FROM usuarios WHERE id = $1',
        [pasajeroId]
      )

      await sendNotification({
        usuario_id: pasajeroId,
        actor_id: usuarioId,
        tipo: 'nueva_oferta',
        titulo: '💰 ¡Nueva oferta recibida!',
        cuerpo: `Un chofer ha respondido a tu viaje con una oferta de ${precio_propuesto} ${moneda || 'CUP'}`,
        datos_extra: { solicitud_id: solicitud_id.toString() },
        fcm_token: userRows.length > 0 ? userRows[0].fcm_token : null
      });
    } catch (notifyErr) {
      console.error('⚠️ Error en proceso de notificación:', notifyErr)
    }

    return created(res, nuevaOferta, '¡Oferta enviada con éxito!')

  } catch (err) {
    next(err)
  }
}

export const aceptarRespuesta = async (req, res, next) => {
  const client = await getClient()
  try {
    const { id: usuarioId, nombre: pasajeroNombre } = req.usuario
    const { respuesta_id } = req.params

    // Iniciar transacción real con el mismo cliente
    await client.query('BEGIN')

    // 1. Obtener datos de la respuesta y verificar propiedad de la solicitud
    const { rows: respRows } = await client.query(
      `SELECT r.*, s.pasajero_id, s.destino_descripcion, u.fcm_token, u.id as chofer_usuario_id
       FROM respuestas_solicitud r
       JOIN solicitudes s ON s.id = r.solicitud_id
       JOIN choferes c ON c.id = r.chofer_id
       JOIN usuarios u ON u.id = c.usuario_id
       WHERE r.id = $1`,
      [respuesta_id]
    )

    if (respRows.length === 0) {
      await client.query('ROLLBACK')
      return notFound(res, 'La oferta no existe')
    }

    const oferta = respRows[0]

    if (oferta.pasajero_id !== usuarioId) {
      await client.query('ROLLBACK')
      return badRequest(res, 'No tienes permiso para aceptar esta oferta')
    }

    // 2. Marcar la oferta como aceptada
    await client.query(
      "UPDATE respuestas_solicitud SET estado = 'aceptado' WHERE id = $1",
      [respuesta_id]
    )

    // 3. Marcar las demás ofertas de la misma solicitud como rechazadas
    // Obtenemos los tokens de los choferes rechazados para notificarles después
    const { rows: rejectedRows } = await client.query(
      `SELECT u.fcm_token, u.id as usuario_id
       FROM respuestas_solicitud r
       JOIN choferes c ON c.id = r.chofer_id
       JOIN usuarios u ON u.id = c.usuario_id
       WHERE r.solicitud_id = $1 AND r.id != $2`,
      [oferta.solicitud_id, respuesta_id]
    )

    await client.query(
      "UPDATE respuestas_solicitud SET estado = 'rechazado' WHERE solicitud_id = $1 AND id != $2",
      [oferta.solicitud_id, respuesta_id]
    )

    // 4. Actualizar la solicitud: asignar chofer y cambiar estado
    await client.query(
      "UPDATE solicitudes SET chofer_seleccionado_id = $1, estado = 'en_proceso' WHERE id = $2",
      [oferta.chofer_id, oferta.solicitud_id]
    )

    await client.query('COMMIT')

    // 5. Notificar al chofer ganador
    // Obtenemos el token más reciente por si acaso
    const { rows: winRows } = await client.query('SELECT fcm_token FROM usuarios WHERE id = $1', [oferta.chofer_usuario_id]);
    const currentToken = winRows.length > 0 ? winRows[0].fcm_token : oferta.fcm_token;

    await sendNotification({
      usuario_id: oferta.chofer_usuario_id,
      actor_id: usuarioId,
      tipo: 'oferta_aceptada',
      titulo: '✅ ¡Oferta aceptada!',
      cuerpo: `${pasajeroNombre} ha aceptado tu oferta para el viaje. ¡Prepárate!`,
      datos_extra: { solicitud_id: oferta.solicitud_id.toString() },
      fcm_token: currentToken
    });

    // 6. Notificar a los choferes rechazados
    if (rejectedRows.length > 0) {
      for (const row of rejectedRows) {
        await sendNotification({
          usuario_id: row.usuario_id,
          actor_id: usuarioId,
          tipo: 'oferta_rechazada',
          titulo: '❌ Oferta no seleccionada',
          cuerpo: 'El pasajero ha aceptado otra oferta para el viaje.',
          datos_extra: { solicitud_id: oferta.solicitud_id.toString() },
          fcm_token: row.fcm_token
        });
      }
    }

    return success(res, null, '¡Viaje confirmado! El chofer ha sido notificado.')
  } catch (err) {
    await client.query('ROLLBACK')
    next(err)
  } finally {
    client.release()
  }
}

/**
 * El pasajero rechaza una oferta individual
 */
export const rechazarRespuesta = async (req, res, next) => {
  try {
    const { id: usuarioId } = req.usuario
    const { respuesta_id } = req.params
    const { motivo } = req.body

    // 1. Obtener datos de la respuesta y verificar propiedad
    const { rows: respRows } = await query(
      `SELECT r.*, s.pasajero_id, s.destino_descripcion, u.fcm_token as chofer_fcm, u.id as chofer_usuario_id
       FROM respuestas_solicitud r
       JOIN solicitudes s ON s.id = r.solicitud_id
       JOIN choferes c ON c.id = r.chofer_id
       JOIN usuarios u ON u.id = c.usuario_id
       WHERE r.id = $1`,
      [respuesta_id]
    )

    if (respRows.length === 0) {
      return notFound(res, 'La oferta no existe')
    }

    const oferta = respRows[0]

    if (oferta.pasajero_id !== usuarioId) {
      return forbidden(res, 'No tienes permiso para rechazar esta oferta')
    }

    // 2. Marcar como rechazado (con motivo si existe)
    try {
      await query(
        "UPDATE respuestas_solicitud SET estado = 'rechazado', motivo_rechazo = $1 WHERE id = $2",
        [motivo || null, respuesta_id]
      )
    } catch (dbErr) {
      // Fallback por si la columna no existe aún
      await query(
        "UPDATE respuestas_solicitud SET estado = 'rechazado' WHERE id = $1",
        [respuesta_id]
      )
    }

    // 3. Notificar al chofer (Persistencia + Push)
    // Obtenemos el token más reciente por si acaso
    const { rows: winRows } = await query('SELECT fcm_token FROM usuarios WHERE id = $1', [oferta.chofer_usuario_id]);
    const currentToken = winRows.length > 0 ? winRows[0].fcm_token : oferta.chofer_fcm;

    await sendNotification({
      usuario_id: oferta.chofer_usuario_id,
      actor_id: usuarioId,
      tipo: 'oferta_rechazada',
      titulo: '❌ Oferta rechazada',
      cuerpo: `Tu oferta ha sido rechazada.${motivo ? ' Motivo: ' + motivo : ''}`,
      datos_extra: { solicitud_id: oferta.solicitud_id.toString() },
      fcm_token: currentToken
    });

    return success(res, null, 'Oferta rechazada correctamente')
  } catch (err) {
    next(err)
  }
}

/**
 * Obtener todas las ofertas de una solicitud específica (Para el pasajero)
 */
export const getOfertasBySolicitud = async (req, res, next) => {
  try {
    const { id: usuarioId } = req.usuario
    const { solicitud_id } = req.params

    // 1. Verificar que la solicitud pertenece al usuario
    const { rows: solRows } = await query(
      'SELECT id FROM solicitudes WHERE id = $1 AND pasajero_id = $2',
      [solicitud_id, usuarioId]
    )

    if (solRows.length === 0) {
      return forbidden(res, 'No tienes permiso para ver las ofertas de esta solicitud')
    }

    // 2. Obtener ofertas con info del chofer y vehículo (solo las no rechazadas)
    const { rows } = await query(
      `SELECT 
        r.*, 
        u.nombre AS chofer_nombre, 
        u.foto_url AS chofer_foto,
        c.calificacion_promedio,
        v.marca, v.placa, v.tipo AS vehiculo_tipo, v.foto_url AS vehiculo_foto
       FROM respuestas_solicitud r
       JOIN choferes c ON c.id = r.chofer_id
       JOIN usuarios u ON u.id = c.usuario_id
       JOIN vehiculos v ON v.id = r.vehiculo_id
       WHERE r.solicitud_id = $1 AND r.estado != 'rechazado'
       ORDER BY r.respondido_en DESC`,
      [solicitud_id]
    )

    return success(res, rows)
  } catch (err) {
    next(err)
  }
}

/**
 * El pasajero cancela su solicitud de viaje
 */
export const cancelarSolicitud = async (req, res, next) => {
  try {
    const { id: usuarioId } = req.usuario
    const { id: solicitudId } = req.params

    // 1. Verificar propiedad y estado
    const { rows } = await query(
      'SELECT id, estado FROM solicitudes WHERE id = $1 AND pasajero_id = $2',
      [solicitudId, usuarioId]
    )

    if (rows.length === 0) {
      return notFound(res, 'Solicitud no encontrada')
    }

    const solicitud = rows[0]
    if (solicitud.estado === 'cancelada') {
      return badRequest(res, 'La solicitud ya está cancelada')
    }
    if (solicitud.estado === 'finalizada') {
      return badRequest(res, 'No se puede cancelar un viaje ya finalizado')
    }

    // 2. Marcar como cancelada
    await query(
      "UPDATE solicitudes SET estado = 'cancelada' WHERE id = $1",
      [solicitudId]
    )

    // 3. (Opcional) Notificar a los choferes que tenían ofertas pendientes
    const { rows: offRows } = await query(
      `SELECT u.id as usuario_id, u.fcm_token
       FROM respuestas_solicitud r
       JOIN choferes c ON c.id = r.chofer_id
       JOIN usuarios u ON u.id = c.usuario_id
       WHERE r.solicitud_id = $1 AND r.estado = 'pendiente'`,
      [solicitudId]
    )

    if (offRows.length > 0) {
      for (const chofer of offRows) {
        await sendNotification({
          usuario_id: chofer.usuario_id,
          actor_id: usuarioId,
          tipo: 'viaje_cancelado',
          titulo: '🚫 Viaje cancelado',
          cuerpo: 'El pasajero ha cancelado la solicitud de viaje.',
          datos_extra: { solicitud_id: solicitudId.toString() },
          fcm_token: chofer.fcm_token
        })
      }
    }

    return success(res, null, 'Solicitud cancelada correctamente')
  } catch (err) {
    next(err)
  }
}
