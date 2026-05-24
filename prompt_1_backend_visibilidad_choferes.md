# PROMPT 1 — Backend: Visibilidad y Ubicación de Choferes en Mapa
**Proyecto:** Te Busco App (Node.js/Express + PostgreSQL)  
**Las columnas nuevas ya fueron agregadas a la BD manualmente.**

---

## CONTEXTO

Se implementa el feature de "choferes visibles en el mapa del pasajero". Las columnas ya existen en `choferes`:
- `visible_en_mapa BOOLEAN DEFAULT false`
- `ultima_lat DOUBLE PRECISION`
- `ultima_lng DOUBLE PRECISION`
- `ultima_ubicacion_en TIMESTAMPTZ`
- `vehiculo_activo_id UUID REFERENCES vehiculos(id) ON DELETE SET NULL`

Un chofer aparece en el mapa del pasajero si cumple **las tres condiciones**:
1. `choferes.estado = 'disponible'` — licencia activa y aprobado
2. `choferes.visible_en_mapa = true` — él decidió activarse
3. `choferes.ultima_ubicacion_en > NOW() - 3 minutos` — está realmente conectado

El chofer elige con qué vehículo aparece al activarse. Eso se guarda en `vehiculo_activo_id` y es lo que ve el pasajero al tocar su marcador.

---

## ARCHIVO NUEVO: `tebusco-api/src/controllers/choferController.js`

Crear este archivo completo:

```javascript
import { query } from '../config/database.js'
import { success, badRequest, forbidden } from '../utils/response.js'

/**
 * PUT /api/choferes/visibilidad
 * El chofer activa o desactiva su visibilidad en el mapa.
 * Body: { visible: true|false, lat, lng, vehiculo_id }
 *
 * - Si visible = true: vehiculo_id es obligatorio si tiene vehículos registrados.
 * - Si visible = false: limpia la ubicación y el vehículo activo.
 */
export const toggleVisibilidad = async (req, res, next) => {
  try {
    const { id: usuarioId, tipo } = req.usuario

    if (tipo !== 'chofer') {
      return forbidden(res, 'Solo los choferes pueden usar esta función')
    }

    const { visible, lat, lng, vehiculo_id } = req.body

    if (typeof visible !== 'boolean') {
      return badRequest(res, 'El campo "visible" debe ser true o false')
    }

    // Obtener chofer
    const { rows: choferRows } = await query(
      `SELECT c.id, c.estado
       FROM choferes c
       WHERE c.usuario_id = $1`,
      [usuarioId]
    )

    if (choferRows.length === 0) {
      return badRequest(res, 'No se encontró el perfil de chofer')
    }

    const chofer = choferRows[0]

    // Si intenta activarse pero no puede operar, rechazar
    if (visible && chofer.estado !== 'disponible') {
      return forbidden(res, 'No puedes activarte en el mapa. Tu licencia está inactiva o tu cuenta no ha sido aprobada.')
    }

    if (visible) {
      // Validar que se envió ubicación
      if (lat == null || lng == null) {
        return badRequest(res, 'Se requiere la ubicación (lat, lng) para activarse')
      }

      // Verificar que el vehículo_id pertenece a este chofer (si se envió)
      if (vehiculo_id) {
        const { rows: vehiculoRows } = await query(
          `SELECT id FROM vehiculos
           WHERE id = $1 AND chofer_id = $2 AND activo = true`,
          [vehiculo_id, chofer.id]
        )
        if (vehiculoRows.length === 0) {
          return badRequest(res, 'El vehículo seleccionado no existe o no te pertenece')
        }
      }

      await query(
        `UPDATE choferes SET
           visible_en_mapa     = true,
           ultima_lat          = $1,
           ultima_lng          = $2,
           ultima_ubicacion_en = NOW(),
           vehiculo_activo_id  = $3
         WHERE id = $4`,
        [lat, lng, vehiculo_id || null, chofer.id]
      )

      return success(res, { visible: true },
        '¡Estás visible! Los pasajeros pueden encontrarte en el mapa.')

    } else {
      // Desactivar: limpiar todo
      await query(
        `UPDATE choferes SET
           visible_en_mapa     = false,
           ultima_ubicacion_en = NULL,
           vehiculo_activo_id  = NULL
         WHERE id = $1`,
        [chofer.id]
      )

      return success(res, { visible: false },
        'Ya no eres visible en el mapa.')
    }
  } catch (err) {
    next(err)
  }
}

/**
 * PUT /api/choferes/ubicacion
 * El ForegroundService del chofer llama a este endpoint cada 60 segundos
 * para mantener actualizada su posición mientras está visible.
 * Body: { lat, lng }
 *
 * Devuelve { debe_detenerse: true } si el chofer fue desactivado remotamente
 * (licencia expirada, admin lo bloqueó, etc.) para que el service se detenga solo.
 */
export const actualizarUbicacion = async (req, res, next) => {
  try {
    const { id: usuarioId, tipo } = req.usuario

    if (tipo !== 'chofer') {
      return forbidden(res, 'Solo los choferes pueden usar esta función')
    }

    const { lat, lng } = req.body

    if (lat == null || lng == null) {
      return badRequest(res, 'Se requieren los campos lat y lng')
    }

    if (typeof lat !== 'number' || typeof lng !== 'number') {
      return badRequest(res, 'lat y lng deben ser números')
    }

    // Validar rango de coordenadas para Cuba
    if (lat < 18 || lat > 24 || lng < -86 || lng > -73) {
      return badRequest(res, 'Coordenadas fuera del rango válido')
    }

    const { rows: choferRows } = await query(
      `SELECT id, estado, visible_en_mapa
       FROM choferes
       WHERE usuario_id = $1`,
      [usuarioId]
    )

    if (choferRows.length === 0) {
      return badRequest(res, 'No se encontró el perfil de chofer')
    }

    const chofer = choferRows[0]

    // Si ya no puede operar, indicarle al ForegroundService que se detenga
    if (!chofer.visible_en_mapa || chofer.estado !== 'disponible') {
      return success(res, { debe_detenerse: true },
        'Tu visibilidad fue desactivada. El servicio de ubicación debe detenerse.')
    }

    await query(
      `UPDATE choferes
       SET ultima_lat = $1, ultima_lng = $2, ultima_ubicacion_en = NOW()
       WHERE id = $3`,
      [lat, lng, chofer.id]
    )

    return success(res, { debe_detenerse: false }, 'Ubicación actualizada')
  } catch (err) {
    next(err)
  }
}

/**
 * GET /api/choferes/disponibles
 * El pasajero obtiene la lista de choferes visibles con ubicación reciente.
 * Solo devuelve choferes cuya última actualización fue hace menos de 3 minutos.
 *
 * Query params opcionales:
 *   lat, lng — posición del pasajero para ordenar por cercanía
 */
export const getChoferesDisponibles = async (req, res, next) => {
  try {
    const { lat, lng } = req.query

    const pasajeroLat = lat ? parseFloat(lat) : null
    const pasajeroLng = lng ? parseFloat(lng) : null

    const distanceSelect = (pasajeroLat && pasajeroLng)
      ? `, ( 6371 * acos( cos(radians($1)) * cos(radians(c.ultima_lat))
            * cos(radians(c.ultima_lng) - radians($2))
            + sin(radians($1)) * sin(radians(c.ultima_lat)) ) ) AS distancia_km`
      : `, NULL AS distancia_km`

    const orderBy = (pasajeroLat && pasajeroLng)
      ? `ORDER BY distancia_km ASC`
      : `ORDER BY c.ultima_ubicacion_en DESC`

    const params = (pasajeroLat && pasajeroLng)
      ? [pasajeroLat, pasajeroLng]
      : []

    const { rows } = await query(`
      SELECT
        c.id            AS chofer_id,
        u.nombre,
        u.telefono,
        c.calificacion_promedio,
        c.total_viajes,
        c.opera_interprovincial,
        c.ultima_lat    AS lat,
        c.ultima_lng    AS lng,
        c.ultima_ubicacion_en,
        -- Vehículo activo elegido por el chofer al activarse
        v.id            AS vehiculo_id,
        v.tipo          AS vehiculo_tipo,
        v.marca         AS vehiculo_marca,
        v.modelo        AS vehiculo_modelo,
        v.color         AS vehiculo_color,
        v.placa         AS vehiculo_placa,
        v.capacidad_pasajeros,
        v.foto_url      AS vehiculo_foto
        ${distanceSelect}
      FROM choferes c
      JOIN usuarios u ON u.id = c.usuario_id
      LEFT JOIN vehiculos v ON v.id = c.vehiculo_activo_id
      WHERE c.estado = 'disponible'
        AND c.visible_en_mapa = true
        AND c.ultima_ubicacion_en > NOW() - INTERVAL '3 minutes'
        AND c.ultima_lat IS NOT NULL
        AND c.ultima_lng IS NOT NULL
      ${orderBy}
      LIMIT 50
    `, params)

    return success(res, rows)
  } catch (err) {
    next(err)
  }
}
```

---

## ARCHIVO NUEVO: `tebusco-api/src/routes/chofer.js`

```javascript
import { Router } from 'express'
import { authenticate, requireVerificado } from '../middleware/auth.js'
import * as choferController from '../controllers/choferController.js'

const router = Router()

router.use(authenticate)

// El chofer activa o desactiva su visibilidad en el mapa
router.put('/visibilidad', requireVerificado, choferController.toggleVisibilidad)

// El ForegroundService actualiza la posición del chofer cada 60s
router.put('/ubicacion', requireVerificado, choferController.actualizarUbicacion)

// El pasajero obtiene choferes visibles cercanos
router.get('/disponibles', choferController.getChoferesDisponibles)

export default router
```

---

## MODIFICAR: `tebusco-api/src/index.js`

Agregar el import junto a los demás imports de rutas:

```javascript
import choferRoutes from './routes/chofer.js'
```

Agregar el `app.use` junto a las demás rutas:

```javascript
app.use('/api/choferes', choferRoutes)
```

---

## RESUMEN

| Archivo | Tipo |
|---|---|
| `tebusco-api/src/controllers/choferController.js` | ✅ Nuevo |
| `tebusco-api/src/routes/chofer.js` | ✅ Nuevo |
| `tebusco-api/src/index.js` | ✏️ 2 líneas |

---

## VERIFICACIÓN

**Activar con vehículo:**
```bash
curl -X PUT http://IP:8005/api/choferes/visibilidad \
  -H "Authorization: Bearer TOKEN_CHOFER" \
  -H "Content-Type: application/json" \
  -d '{"visible": true, "lat": 22.1547, "lng": -80.4446, "vehiculo_id": "UUID_DEL_VEHICULO"}'
```

**Actualizar ubicación (simula el ForegroundService):**
```bash
curl -X PUT http://IP:8005/api/choferes/ubicacion \
  -H "Authorization: Bearer TOKEN_CHOFER" \
  -H "Content-Type: application/json" \
  -d '{"lat": 22.1548, "lng": -80.4447}'
```

**Ver choferes disponibles (como pasajero):**
```bash
curl -X GET "http://IP:8005/api/choferes/disponibles?lat=22.155&lng=-80.444" \
  -H "Authorization: Bearer TOKEN_PASAJERO"
```
