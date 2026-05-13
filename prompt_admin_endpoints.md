# Prompt: Endpoints de Administración — Te Busco API

Eres un desarrollador backend senior. Voy a darte el contexto completo de un proyecto
y necesito que implementes un módulo nuevo sin modificar nada de lo que ya existe.

---

## CONTEXTO DEL PROYECTO

API REST en Node.js con Express 5, ES Modules, PostgreSQL (cliente `pg`), y Firebase Admin
para notificaciones push. Estructura relevante:

```
src/
  controllers/
    authController.js
    solicitudController.js
    vehicleController.js
    statsController.js        ← tiene getPublicStats con 4 métricas básicas
    notificationController.js
  middleware/
    auth.js                   ← exporta: authenticate, authorize, requireVerificado
  services/
    notificationService.js    ← exporta: sendNotification(...)
  utils/
    response.js               ← exporta: success, created, badRequest, notFound,
                                          forbidden, unauthorized, conflict, error
  config/
    database.js               ← exporta: query, getClient
  routes/
    auth.js, solicitud.js, vehicle.js, notification.js
  index.js                    ← punto de entrada, registra todos los routers
```

### Middleware de autorización (ya existe, úsalo exactamente así):
- `authenticate` — verifica JWT y adjunta `req.usuario = { id, nombre, tipo, verificado, provincia_id, municipio_id }`
- `authorize(...tipos)` — verifica que `req.usuario.tipo` esté en la lista
- Ejemplo de ruta protegida para admin: `router.use(authenticate, authorize('admin'))`

### sendNotification (firma exacta):
```js
await sendNotification({
  usuario_id,     // UUID del receptor — null si es tópico
  actor_id,       // UUID de quien actúa — null si es el sistema
  tipo,           // valor del enum tipo_notificacion_v2
  titulo,         // string
  cuerpo,         // string
  datos_extra,    // objeto plano { clave: valor }
  fcm_token,      // string | null
  topic           // string | null — para envíos masivos por tema FCM
})
```
Maneja internamente tanto la persistencia en BD como el push FCM. Nunca lanzar errores
al flujo principal — envolverla siempre en try/catch propio con console.error.

### response.js — helpers disponibles:
```
success(res, data, message) | created(res, data, message) | badRequest(res, message) |
notFound(res, message) | forbidden(res, message) | conflict(res, message)
```

### Patrón de transacción del proyecto (síguelo cuando haya múltiples operaciones):
```js
const client = await getClient()
try {
  await client.query('BEGIN')
  // operaciones...
  await client.query('COMMIT')
  return success(res, data, 'Mensaje')
} catch (err) {
  await client.query('ROLLBACK')
  next(err)
} finally {
  client.release()
}
```

---

## TABLAS Y ENUMS RELEVANTES

```
usuarios:    id (uuid), nombre, username, telefono, email, tipo (enum),
             foto_url, activo (bool), verificado (bool),
             provincia_id, municipio_id, fecha_registro, fcm_token

choferes:    id (uuid), usuario_id, licencia_numero, licencia_foto_url,
             estado (enum: disponible|ocupado|inactivo),
             calificacion_promedio, total_viajes,
             provincia_base_id, municipio_base_id,
             opera_interprovincial (bool),
             aprobado_por (uuid→usuarios), aprobado_en (timestamptz)

solicitudes: id, pasajero_id, chofer_seleccionado_id, estado (enum),
             origen_descripcion, destino_descripcion, distancia,
             precio_oferta, moneda, creada_en, completada_en

valoraciones: id, solicitud_id, pasajero_id, chofer_id,
              estrellas, comentario, creada_en

tipo_usuario enum:     pasajero | chofer | admin
estado_chofer enum:    disponible | ocupado | inactivo
estado_solicitud enum: activa | en_proceso | completada | cancelada | expirada
tipo_notificacion_v2:  nueva_solicitud | nueva_oferta | oferta_aceptada |
                       oferta_rechazada | viaje_confirmado | viaje_cancelado |
                       viaje_completado | chofer_llegada | sistema_alerta | mensaje_chat
```

---

## MÓDULO A CREAR

Crear dos archivos nuevos: `src/controllers/adminController.js` y `src/routes/admin.js`.
Luego registrar el router en `src/index.js` bajo `/api/admin`.

Todas las rutas de admin deben estar protegidas con `authenticate` + `authorize('admin')`.

---

## ENDPOINTS A IMPLEMENTAR

### 1. ESTADÍSTICAS — `GET /api/admin/stats`

Devuelve métricas completas del panel. En una sola función con queries paralelas
(`Promise.all`), retornar:

| Campo | Query |
|---|---|
| `total_usuarios` | usuarios con tipo != 'admin' |
| `total_pasajeros` | usuarios con tipo = 'pasajero' |
| `total_choferes` | total de registros en tabla choferes |
| `choferes_pendientes` | choferes con estado = 'inactivo' Y aprobado_en IS NULL |
| `choferes_activos` | choferes con estado = 'disponible' u 'ocupado' |
| `viajes_completados` | solicitudes con estado = 'completada' |
| `viajes_activos` | solicitudes con estado IN ('activa', 'en_proceso') |
| `viajes_cancelados` | solicitudes con estado = 'cancelada' |
| `valoracion_promedio_plataforma` | AVG(estrellas) de toda la tabla valoraciones, redondeado a 2 decimales, como número (no string) |

---

### 2. GESTIÓN DE CHOFERES

#### `GET /api/admin/choferes`
Lista todos los choferes con info del usuario asociado. Query params opcionales:
- `estado` — filtrar por estado_chofer (`disponible|ocupado|inactivo`)
- `pendiente` — si vale `'1'` o `'true'`, filtrar solo los que tienen `aprobado_en IS NULL`
- `page` y `limit` — paginación (límite máx 50, default 20; page default 1)

Campos a devolver por chofer:
```sql
c.id, c.estado, c.calificacion_promedio, c.total_viajes,
c.opera_interprovincial, c.licencia_numero, c.aprobado_en,
u.id as usuario_id, u.nombre, u.username, u.telefono, u.email,
u.foto_url, u.activo, u.verificado, u.fecha_registro, u.fcm_token,
p.nombre as provincia, m.nombre as municipio
```
JOIN con `usuarios`, LEFT JOIN con `provincias` y `municipios` por `municipio_base_id`.
Ordenado por `fecha_registro DESC`.
Formato de respuesta: `{ data: [...], total, page, limit }`.

---

#### `GET /api/admin/choferes/:id`
Detalle completo de un chofer. Mismo JOIN que el listado, más:
- Sus vehículos: `SELECT * FROM vehiculos WHERE chofer_id = $1`
- Sus últimas 5 valoraciones: JOIN con `solicitudes` y `usuarios` (pasajero) para mostrar nombre del pasajero, estrellas y comentario
- Sus últimas 10 solicitudes completadas

---

#### `POST /api/admin/choferes/:id/aprobar`
Aprueba un chofer. En transacción:
1. Verificar que el chofer existe y su estado es `'inactivo'`.
2. Actualizar `choferes`: `estado = 'disponible'`, `aprobado_por = req.usuario.id`, `aprobado_en = NOW()`.
3. Actualizar `usuarios`: `verificado = true`.
4. COMMIT.
5. **Fuera de la transacción:** notificación push al chofer:
   - `tipo`: `'sistema_alerta'`
   - `titulo`: `'✅ ¡Cuenta aprobada!'`
   - `cuerpo`: `'¡Buenas noticias! Tu cuenta de chofer ha sido aprobada. Ya puedes recibir solicitudes de viaje.'`
   - `fcm_token` del usuario del chofer (consultar antes del COMMIT)

---

#### `POST /api/admin/choferes/:id/rechazar`
Body opcional: `{ motivo: string }`. En transacción:
1. Verificar que el chofer existe.
2. Actualizar `choferes`: `estado = 'inactivo'`, `aprobado_por = NULL`, `aprobado_en = NULL`.
3. Actualizar `usuarios`: `verificado = false`.
4. COMMIT.
5. **Fuera de la transacción:** notificación push al chofer:
   - `tipo`: `'sistema_alerta'`
   - `titulo`: `'❌ Cuenta no aprobada'`
   - `cuerpo` dinámico:
     - Con motivo: `'Tu cuenta de chofer no fue aprobada. Motivo: ${motivo}'`
     - Sin motivo: `'Tu cuenta de chofer no fue aprobada. Contacta al soporte para más información.'`

---

### 3. GESTIÓN DE USUARIOS

#### `GET /api/admin/usuarios`
Lista todos los usuarios excepto admins. Query params opcionales:
- `tipo` — filtrar por tipo_usuario (`pasajero|chofer`)
- `activo` — `'1'`/`'true'` o `'0'`/`'false'`
- `search` — búsqueda por nombre, username, email o teléfono (`ILIKE %search%`)
- `page` y `limit` (mismo comportamiento que en choferes)

Campos: `id, nombre, username, telefono, email, tipo, foto_url, activo, verificado, fecha_registro, provincia, municipio` (JOIN con `provincias` y `municipios`).
Ordenado por `fecha_registro DESC`. Mismo formato de respuesta paginada.

---

#### `PATCH /api/admin/usuarios/:id/toggle-activo`
Activa o desactiva una cuenta de usuario.
1. Verificar que el usuario existe y no es admin (un admin no puede ser desactivado).
2. Hacer toggle: `activo = NOT activo`.
3. Devolver el nuevo estado en la respuesta.
4. **Fuera de la transacción:** notificación push según resultado:
   - Si se **desactiva**: `titulo: '⚠️ Cuenta suspendida'`, `cuerpo: 'Tu cuenta ha sido suspendida temporalmente. Contacta al soporte.'`
   - Si se **activa**: `titulo: '✅ Cuenta reactivada'`, `cuerpo: 'Tu cuenta ha sido reactivada. Ya puedes usar Te Busco con normalidad.'`

---

### 4. MONITOREO DE SOLICITUDES

#### `GET /api/admin/solicitudes`
Lista solicitudes con info de pasajero y chofer. Query params opcionales:
- `estado` — filtrar por estado_solicitud
- `page` y `limit`

Usar la vista `v_solicitudes` que ya existe en la BD más:
```sql
LEFT JOIN usuarios uc ON uc.id = s.chofer_seleccionado_id
```
para obtener el nombre del chofer asignado. Ordenado por `creada_en DESC`. Mismo formato paginado.

---

### 5. NOTIFICACIONES MASIVAS

#### `POST /api/admin/notificaciones/broadcast`
Envía una notificación a todos los usuarios o filtrada por tipo.

Body:
```json
{
  "titulo": "string (requerido)",
  "cuerpo": "string (requerido)",
  "tipo_usuario": "pasajero | chofer (opcional — si no viene, se envía a todos excepto admins)"
}
```

1. Validar que `titulo` y `cuerpo` no estén vacíos → `badRequest` si faltan.
2. Obtener todos los usuarios según el filtro con su `fcm_token`.
3. Para cada usuario, llamar a `sendNotification` con `tipo: 'sistema_alerta'`.
   Usar `Promise.allSettled` para que un fallo individual no detenga los demás.
4. Devolver `{ enviadas: N, fallidas: M }` en la respuesta.

---

## REGISTRAR EN index.js

Añadir después de los demás routers existentes:
```js
import adminRoutes from './routes/admin.js'
// ...
app.use('/api/admin', adminRoutes)
```

---

## REGLAS GENERALES

- ES Modules en todo (`import`/`export`), sin CommonJS.
- Cada función exportada como `export const nombreFuncion = async (req, res, next) => {...}`.
- Todo error de BD inesperado se pasa a `next(err)`, nunca se traga silenciosamente.
- Las notificaciones push siempre van **fuera de la transacción** y en su propio `try/catch`.
- No modificar ningún archivo existente salvo añadir el import y la línea `app.use('/api/admin', adminRoutes)` en `index.js`.
- No añadir nuevas dependencias npm — usar solo lo que ya está instalado en el proyecto.

---

## ENTREGABLES

1. `src/controllers/adminController.js` — todas las funciones del módulo
2. `src/routes/admin.js` — router con todas las rutas protegidas
3. Fragmento a añadir en `src/index.js` (import + app.use)
