# 🚕 AUDITORÍA TÉCNICA Y ESTRATÉGICA — Te Busco App
**Nivel: CTO / Arquitecto Senior | Fecha: Mayo 2026**  
**Repositorio:** `github.com/roloramros/teBusco_app`  
**Stack:** Android (Java) · Node.js/Express · PostgreSQL · Firebase FCM · React Admin

---

## VEREDICTO EJECUTIVO

Te Busco es un MVP con ambición real. La idea de un marketplace de transporte para Cuba es válida y diferenciada. El código muestra criterio: hay transacciones en BD, rate limiting, middleware de auth, gestión de sesiones multi-dispositivo, notificaciones por FCM, y un panel admin funcional. Eso no es trivial.

**Pero hay problemas que en producción con miles de usuarios van a destruir el producto.** Algunos son bombas de tiempo. Otros son deudas técnicas que se acumulan y te atraparán en 3 meses si no actúas antes.

**El proyecto necesita 4–6 semanas de trabajo focalizado antes de cualquier escalado serio.** No para reescribir nada, sino para blindar lo que ya existe.

---

## SECCIÓN 1 — VULNERABILIDADES DE SEGURIDAD CRÍTICAS

### 🔴 1.1 API Key de Google Maps expuesta en repositorio público

**Severidad: CRÍTICA. Impacto económico directo.**

```java
// MainActivity.java
Places.initialize(getApplicationContext(), "AIzaSyBufiSwuBW19JLsbXKDbW86pg_1wL7ifxU");

// GeocodingRepository.java + MapRepository.java
private static final String API_KEY = "AIzaSyBufiSwuBW19JLsbXKDbW86pg_1wL7ifxU";

// AndroidManifest.xml
android:value="AIzaSyBufiSwuBW19JLsbXKDbW86pg_1wL7ifxU"
```

Esta clave aparece **cuatro veces en el repositorio público**. Cualquier bot de GitHub que escanee claves (y existen muchos, incluido el propio GitHub Secret Scanning) ya la encontró. Con ella alguien puede hacer miles de llamadas a Maps API a tu costo. Las facturas de Google pueden ser devastadoras.

**Acciones inmediatas:**
1. Ir a Google Cloud Console → Credentials → Revocar esta key ahora mismo
2. Crear una nueva key con restricciones: solo tu `applicationId` (`com.codram.terecojo`) y solo las APIs que usas (Maps SDK, Places API, Directions API)
3. Mover la key a `local.properties` (ya en `.gitignore`) y leerla desde `build.gradle.kts`
4. No hardcodear nunca más en código Java o XML

```kotlin
// build.gradle.kts — forma correcta
val mapsKey = localProperties.getProperty("MAPS_API_KEY") ?: ""
buildConfigField("String", "MAPS_API_KEY", "\"$mapsKey\"")
resValue("string", "maps_api_key", mapsKey)
```

---

### 🔴 1.2 URL del servidor de producción expuesta en código

**Severidad: ALTA**

```kotlin
// build.gradle.kts — expuesto públicamente
debug {
    buildConfigField("String", "API_BASE_URL", "\"http://69.169.102.33:8005/\"")
}
release {
    buildConfigField("String", "API_BASE_URL", "\"http://69.169.102.33:8004/\"")
}
```

La IP de tu VPS está pública. Además, estás usando **HTTP puro** (sin SSL), lo que significa que todo el tráfico entre la app y el servidor viaja en texto plano: tokens JWT, contraseñas, datos de usuarios. Un ataque man-in-the-middle (muy posible en redes Wi-Fi públicas) expone todo.

**Acciones:**
1. Configurar HTTPS en el servidor (Let's Encrypt + Nginx, es gratis)
2. Mover la IP/dominio a `local.properties` o a variables de entorno del CI/CD
3. Activar `android:usesCleartextTraffic="false"` en el Manifest (actualmente está en `true`)
4. Agregar `network_security_config.xml` que solo permita HTTPS en producción

---

### 🔴 1.3 CORS abierto en producción

**Severidad: ALTA**

```javascript
// index.js
origin: process.env.NODE_ENV === 'production'
    ? process.env.FRONTEND_URL || '*'   // ← El || '*' es la trampa
    : '*',
```

Si `FRONTEND_URL` no está configurada en el servidor (olvidaste ponerla, el VPS se reinició, lo que sea), el CORS cae a `'*'`, aceptando peticiones desde cualquier origen. Esto permite ataques CSRF desde cualquier sitio web.

**Fix:**
```javascript
const allowedOrigins = process.env.ALLOWED_ORIGINS?.split(',') || []
origin: (origin, callback) => {
    if (!origin || allowedOrigins.includes(origin)) callback(null, true)
    else callback(new Error('Not allowed by CORS'))
}
```

---

### 🟡 1.4 Rate limiting insuficiente para el radar de viajes

El endpoint `GET /api/solicitudes/radar` (que los choferes usan para ver solicitudes activas) no tiene rate limiting específico, solo el global de 100 req/15min. Un scraper puede llamarlo en loop y mapear toda la actividad de pasajeros en tiempo real. Esto es un problema de privacidad además de carga.

**Fix:** Rate limiting específico por usuario autenticado + caché de 30 segundos en ese endpoint.

---

### 🟡 1.5 Tokens JWT de 7 días sin invalidación eficiente

```javascript
const EXPIRES_IN = process.env.JWT_EXPIRES_IN || '7d'
```

El sistema guarda tokens en la BD (`sesiones`), lo cual es correcto. Pero si la tabla de sesiones crece a millones de filas (usuarios activos × 3 sesiones × 30 días), la consulta de auth (`SELECT ... FROM sesiones WHERE token = $1`) puede degradarse sin un índice correcto sobre `token`. ¿Existe ese índice en tu schema? No lo veo en el código subido.

**Fix:** Asegurar `CREATE INDEX idx_sesiones_token ON sesiones(token)` y `CREATE INDEX idx_sesiones_expira ON sesiones(expira_en)` para el job de limpieza.

---

### 🟡 1.6 Eliminación de usuario sin anonimización (GDPR/privacidad)

```javascript
// adminController.js
await client.query('DELETE FROM valoraciones WHERE pasajero_id = $1', [id])
await client.query('DELETE FROM solicitudes WHERE pasajero_id = $1', [id])
await client.query('DELETE FROM usuarios WHERE id = $1', [id])
```

Al eliminar un usuario se borran sus solicitudes y valoraciones en cascada. Esto rompe la integridad histórica: los choferes pierden sus ratings, el historial de viajes desaparece para auditoría. La práctica correcta no es borrar, sino **anonimizar**: poner nombre="Usuario eliminado", nullear email/teléfono, marcar como `eliminado=true`.

Además, intentas notificar al usuario **después** de eliminarlo (el FCM token ya fue borrado con el usuario). La notificación nunca llega.

---

## SECCIÓN 2 — PROBLEMAS DE ARQUITECTURA Y ESCALABILIDAD

### 🔴 2.1 Polling implícito: El chofer no tiene tiempo real

El radar de solicitudes (`/api/solicitudes/radar`) es un endpoint REST que el cliente debe consultar periódicamente. No hay WebSockets, no hay Server-Sent Events. Esto significa que:

- El chofer se entera de una nueva solicitud solo cuando el polling dispara
- Con 100 choferes haciendo polling cada 10 segundos = 600 requests/minuto solo para el radar
- No escala. Con 1,000 choferes activos son 6,000 req/min solo para una feature

La app ya usa FCM para notificaciones push, que es el canal correcto. Pero el flujo debería ser:
**Nueva solicitud → Push FCM al municipio (ya implementado) → El chofer recibe y abre la app → Single API call para ver detalles**. No polling.

**Solución a mediano plazo:** WebSockets (Socket.io) o Server-Sent Events para el radar. Mientras tanto, aumentar el intervalo de polling y confiar más en FCM.

---

### 🔴 2.2 El cron job de expiración vive dentro del servidor Express

```javascript
// expireSolicitudes.js
cron.schedule('*/30 * * * *', async () => { ... })
startExpireSolicitudesJob() // llamado dentro de app.listen()
```

Este diseño tiene dos problemas graves:
1. **Si corres múltiples instancias del servidor** (horizontal scaling, PM2 cluster mode), el job se ejecuta N veces simultáneamente → condiciones de carrera, solicitudes expiradas doble notificación
2. **Si el servidor cae y tarda 45 minutos en reiniciarse**, las solicitudes no se expiran durante ese tiempo

**Solución:** Mover el cron a un worker separado, o usar una solución de cola de trabajos (BullMQ + Redis). Para tu escala actual, al menos agregar un lock distribuido o usar un flag de DB (`en_proceso_expiracion`).

---

### 🔴 2.3 Imágenes de vehículos almacenadas en el filesystem del servidor

```javascript
// upload.js
destination: path.join(__dirname, '../public/uploads/vehicles')
```

Las fotos se guardan en el disco local del VPS. Esto significa:
- Si el VPS muere, pierdes todas las fotos
- Si escalar a 2 servidores, cada uno tiene fotos distintas
- El servidor Express sirve archivos estáticos (costoso en CPU)
- Sin CDN, las imágenes viajan lentas para usuarios lejos del servidor

**Solución:** Usar Cloudinary (gratis hasta 25GB) o AWS S3. Para Cuba especialmente, un CDN puede marcar la diferencia en velocidad de carga.

---

### 🟡 2.4 N+1 Queries en el job de expiración

```javascript
// expireSolicitudes.js — dentro del loop de solicitudes expiradas
const { rows: userRows } = await query(
    'SELECT fcm_token FROM usuarios WHERE id = $1',
    [fila.pasajero_id]
)
```

Por cada solicitud expirada, se hace una query adicional para obtener el FCM token. Con 50 solicitudes expirando simultáneamente son 51 queries donde podrían ser 2 (una para expirar, una para obtener todos los tokens en batch).

```sql
-- Versión optimizada: una sola query
UPDATE solicitudes s
SET estado = 'expirada'
FROM usuarios u
WHERE u.id = s.pasajero_id
  AND s.estado = 'activa'
  AND (condicion de tiempo)
RETURNING s.id, s.pasajero_id, u.fcm_token
```

---

### 🟡 2.5 Pool de conexiones configurado para VPS pequeño, no para producción

```javascript
max: 10,  // máximo 10 conexiones simultáneas
```

Con 10 conexiones y un pico de 200 usuarios concurrentes, cada request puede esperar hasta 5 segundos en obtener una conexión del pool. PostgreSQL en un VPS pequeño típicamente puede manejar 50-100 conexiones eficientemente. 

Además, no hay configuración de `statement_timeout` para matar queries lentas, ni `application_name` para identificar conexiones en pg_stat_activity.

---

### 🟡 2.6 Sin sistema de caché

Ningún endpoint usa caché. Las consultas más frecuentes y costosas se ejecutan en raw SQL cada vez:

- `GET /api/geo/provincias` y `/municipios` → datos estáticos, se consultan en cada registro/login
- `GET /api/auth/me` → se llama en cada auto-login (cada vez que el usuario abre la app)
- `GET /api/geo/stats` → se llama desde la pantalla de login de todos los usuarios

Con Redis + caché de 5 minutos en datos estáticos, puedes reducir el 40% de las queries.

---

## SECCIÓN 3 — ERRORES DE DISEÑO LÓGICO

### 🔴 3.1 El pasajero puede ver solicitudes de otros pasajeros

`GET /api/solicitudes/:id` no verifica que la solicitud pertenezca al usuario autenticado:

```javascript
export const getSolicitudById = async (req, res, next) => {
    const { id } = req.params
    const { rows } = await query(
        'SELECT * FROM v_solicitudes WHERE id = $1',
        [id]  // ← No filtra por req.usuario.id
    )
    // ...
}
```

Cualquier pasajero autenticado puede ver los detalles de la solicitud de otro usuario (origen, destino, precio ofertado, número de pasajeros). Si la vista incluye datos personales, esto es una violación de privacidad.

**Fix:**
```javascript
WHERE id = $1 AND (pasajero_id = $2 OR /* es chofer */ tipo = 'chofer')
```

---

### 🔴 3.2 El chofer puede ver todas las solicitudes activas de todos los municipios si no tiene municipio asignado

```javascript
// solicitudController.js
if (municipio_id) {
    sql += ` AND s.origen_municipio_id = $${params.length + 1}`;
    params.push(municipio_id);
}
// Si municipio_id es null, no hay filtro → devuelve TODO
```

Un chofer sin municipio asignado ve **todas las solicitudes activas de toda Cuba**. Puede ser intencional para choferes interprovinciales, pero no está documentado y puede revelar datos de otras regiones sin consentimiento.

---

### 🟡 3.3 Flujo de finalización asimétrico: solo el pasajero puede finalizar

El pasajero finaliza el viaje y califica al chofer. Pero ¿qué pasa si el pasajero nunca finaliza el viaje? El viaje queda en estado `en_proceso` indefinidamente. No hay:
- Timeout automático de viajes en progreso
- Capacidad del chofer para marcar el viaje como completado
- Calificación del chofer hacia el pasajero (el chofer no puede valorar al pasajero)

Esto afecta las métricas, la reputación del sistema y deja datos corruptos en producción.

---

### 🟡 3.4 La cancelación del chofer no tiene penalización ni registro

```javascript
// cancelarViajeChofer: solo actualiza estado, no registra el evento
await client.query(
    "UPDATE solicitudes SET estado = 'activa', chofer_seleccionado_id = NULL WHERE id = $1",
    [solicitudId]
)
```

Un chofer puede aceptar viajes y cancelarlos repetidamente sin consecuencias. No hay contador de cancelaciones, no hay tiempo mínimo entre cancelaciones, no hay penalización. Esto puede ser explotado o simplemente crear mala experiencia para pasajeros.

---

### 🟡 3.5 `getTodasSolicitudesActivas` hace query extra innecesaria por diseño

```javascript
// Primero busca el choferId
const { rows: choferRows } = await query('SELECT id FROM choferes WHERE usuario_id = $1', ...)
const choferId = choferRows.length > 0 ? choferRows[0].id : null;
// Luego lo usa como parámetro
LEFT JOIN respuestas_solicitud r ON r.solicitud_id = v.id AND r.chofer_id = $1
```

Si el usuario no es chofer (`choferId = null`), el `LEFT JOIN` con `r.chofer_id = null` devuelve siempre false, lo cual técnicamente funciona pero es ineficiente y semánticamente incorrecto. ¿Por qué un pasajero accedería al radar? Las rutas no están suficientemente protegidas por rol.

---

## SECCIÓN 4 — PROBLEMAS DE RENDIMIENTO

### 🟡 4.1 `getPublicStats` ejecuta 4 queries secuenciales con for-of

```javascript
// statsController.js
for (const [key, sql] of Object.entries(queries)) {
    const { rows } = await query(sql)  // ← SECUENCIAL
    results[key] = rows[0].count
}
```

Cuatro queries que se ejecutan una después de otra. Con `Promise.all()` serían 4 en paralelo → 4x más rápido. Este endpoint se llama en cada pantalla de login de cada usuario.

---

### 🟡 4.2 `adminController.getStats` ejecuta 9 queries secuenciales

```javascript
const results = await Promise.all(
    Object.entries(queries).map(async ([key, sql]) => {
        const { rows } = await query(sql, params)
```

Aquí sí usa `Promise.all`, bien. Pero las 9 queries son independientes y podrían reemplazarse por **una sola query SQL** con múltiples `COUNT` usando `FILTER`:

```sql
SELECT
    COUNT(*) FILTER (WHERE tipo != 'admin') as total_usuarios,
    COUNT(*) FILTER (WHERE tipo = 'pasajero') as total_pasajeros,
    COUNT(*) FILTER (WHERE tipo = 'chofer') as total_choferes
FROM usuarios;
```

Esto es ~9x más eficiente.

---

### 🟡 4.3 Sin paginación en endpoints críticos del chofer

`getMisViajesChofer` y `getMisSolicitudes` devuelven **todos** los viajes sin paginación. Un chofer con 500 viajes completados recibe 500 registros en una sola respuesta. Con 1000 usuarios activos esto puede saturar la BD y la red.

---

## SECCIÓN 5 — PROBLEMAS DE CÓDIGO Y MANTENIBILIDAD

### 5.1 La app Android está en Java, debería migrar a Kotlin

El estándar oficial de Android desde 2019 es Kotlin. Java en Android no recibe nuevas APIs de Jetpack de forma prioritaria. El ecosistema de librerías, tutoriales, y ejemplos está en Kotlin. Esto complica contratar colaboradores, usar librerías modernas (Compose, Coroutines nativas) y mantener el código a largo plazo.

**No es urgente, pero sí es deuda que crece.**

---

### 5.2 Arquitectura Android parcialmente implementada (MVVM incompleto)

Tienes `MainViewModel` y `DriverViewModel`, pero la mayoría de las Activities hacen llamadas directas a `RetrofitClient.getService()` sin pasar por ViewModels:

```java
// LoginActivity.java — llamada directa sin ViewModel
RetrofitClient.getService().login(request).enqueue(new Callback<...>() { ... })
```

Esto mezcla lógica de negocio con lógica de UI, hace el código difícil de testear y puede causar memory leaks si los callbacks capturan referencias a Activities ya destruidas.

---

### 5.3 Callback hell en la app Android

El código de `LoginActivity` anida callbacks dentro de callbacks:

```java
FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
    String token = task.getResult();
    RetrofitClient.getService().updateFcmToken(...).enqueue(new Callback<...>() {
        // otro nivel más de callbacks
    });
});
```

Con Kotlin Coroutines o RxJava esto sería lineal y legible. En Java, este patrón escala muy mal.

---

### 5.4 24 `console.log` en producción

La API tiene 24 `console.log` activos que nunca se suprimen en producción. En un servidor con miles de requests esto:
- Llena los logs con ruido
- Tiene un costo de I/O real
- Puede exponer datos sensibles en logs

No hay una librería de logging estructurado (Winston, Pino). Sin logs estructurados no puedes hacer alertas automáticas ni análisis de errores en producción.

---

### 5.5 Un solo archivo de test, y no prueba los flujos más críticos

El único test prueba `/api/geo/stats`. No hay tests para:
- Registro/login de usuarios
- Creación de solicitudes
- Aceptación de ofertas (el flujo más complejo con transacciones)
- Expiración de solicitudes
- Autorización (¿puede un pasajero acceder a rutas de chofer?)

---

### 5.6 `isMinifyEnabled = false` en build de release

```kotlin
release {
    isMinifyEnabled = false  // ← Error grave
}
```

El APK de producción no está ofuscado ni optimizado. Esto:
- Hace que el APK sea 2-3x más grande de lo necesario
- Permite reverse engineering trivial del código (ver endpoints, lógica de negocio)
- Deshabilita las optimizaciones de Proguard/R8 que eliminan código muerto

---

### 5.7 El release usa `signingConfig = signingConfigs.getByName("debug")`

```kotlin
release {
    signingConfig = signingConfigs.getByName("debug")  // ← NUNCA en producción
}
```

Estás firmando el APK de producción con el keystore de debug. Si alguien descarga tu APK y extrae el debug keystore (que es público y estándar de Android), puede firmar APKs maliciosos que podrían reemplazar tu app en dispositivos donde la tienen instalada.

---

## SECCIÓN 6 — UX/UI Y EXPERIENCIA DE USUARIO

### 6.1 Sin feedback de estado en tiempo real para el pasajero

El flujo del pasajero tiene un gap enorme: publica solicitud → espera → ¿cuándo sabe que llegó una oferta? Solo por notificación push. Si la notificación no llega (dispositivo sin internet, notificaciones desactivadas), el pasajero no sabe nada. No hay un indicador de "tienes X ofertas nuevas" en la UI sin reabrir la pantalla.

---

### 6.2 Sin recuperación de contraseña

No hay endpoint ni flujo de "Olvidé mi contraseña". Con usuarios que usan teléfono como identificador (sin email obligatorio), si olvidan la contraseña, están bloqueados permanentemente. Esto es un killer de retención.

---

### 6.3 Sin perfil de foto de usuario (solo vehículos)

Los vehículos tienen foto, los usuarios no pueden subir foto de perfil. En una app de transporte, la foto del chofer genera confianza. Es una de las primeras cosas que los usuarios esperan.

---

### 6.4 Sin chat entre pasajero y chofer

Después de que se acepta una oferta, el único canal de comunicación es el teléfono (que se comparte via notificación). No hay mensajería in-app. Esto:
- Expone números de teléfono personales
- Si el número cambia, no hay forma de contactar
- No hay historial de comunicación

---

### 6.5 Sin mapa en tiempo real de la ubicación del chofer

Una vez aceptada la oferta, el pasajero no puede ver dónde está el chofer. La experiencia es: "acepté la oferta, ahora llamo por teléfono para coordinar". Esto es el año 2015.

---

### 6.6 El panel admin carece de funciones operativas críticas

El panel admin puede ver estadísticas, listar choferes, aprobar/rechazar. Pero falta:
- **Mapa de solicitudes activas en tiempo real**
- **Buscador global** (por nombre, teléfono, placa)
- **Historial de acciones del admin** (quién aprobó qué y cuándo)
- **Exportar datos a CSV/Excel** para análisis
- **Gestión de reportes y quejas** de usuarios
- **Configuración dinámica** (cambiar límite de sesiones, tiempos de expiración sin redeploy)

---

## SECCIÓN 7 — FUNCIONES FALTANTES PARA UN PRODUCTO PROFESIONAL

### Críticas (sin ellas el producto está incompleto)
| Feature | Estado | Impacto |
|---|---|---|
| Recuperación de contraseña | ❌ Ausente | Crítico — usuarios bloqueados |
| HTTPS en toda la app | ❌ HTTP plano | Crítico — datos expuestos |
| Foto de perfil de usuario | ❌ Ausente | Alto — confianza |
| Valoración bidireccional | ❌ Solo pasajero→chofer | Alto — sistema de reputación incompleto |
| Timeout de viajes en_proceso | ❌ Ausente | Alto — datos corruptos |
| Tests de flujos críticos | ❌ 1 solo test | Alto — sin red de seguridad |

### Importantes (para competir profesionalmente)
| Feature | Estado | Impacto |
|---|---|---|
| Chat in-app | ❌ Ausente | Alto — privacidad y UX |
| Seguimiento en mapa del chofer | ❌ Ausente | Alto — estándar del sector |
| Notificaciones in-app / badge | ❌ Solo push | Medio |
| Historial de búsquedas frecuentes | ❌ Ausente | Medio |
| Filtros avanzados en el radar | ❌ Solo municipio | Medio |
| Panel admin con mapa | ❌ Ausente | Medio |
| Modo offline básico | ❌ Ausente | Medio |
| Exportación de datos | ❌ Ausente | Medio |

---

## SECCIÓN 8 — MONETIZACIÓN Y MODELO DE NEGOCIO

### 8.1 El modelo actual no tiene mecanismo de ingresos

La app no tiene ninguna lógica de cobro. Para monetizar, tienes varias opciones viables para el mercado cubano:

**Tier 1 — Comisión por viaje completado:**
- Cobrar un % al chofer por cada viaje exitoso
- Requiere integrar un sistema de pagos (complejo en Cuba)
- Alternativa: sistema de créditos prepagados

**Tier 2 — Suscripción para choferes:**
- Plan Básico (gratis): X solicitudes/mes visibles
- Plan Pro ($5-10/mes): solicitudes ilimitadas, prioridad en el radar, badge verificado
- Plan Premium: acceso a rutas interprovinciales, estadísticas de mercado

**Tier 3 — Publicidad contextual:**
- Mostrar negocios locales relevantes en el mapa mientras el usuario espera
- Bajo en fricción, no requiere pagos complejos

**Tier 4 — Datos y estadísticas de mercado:**
- Vender informes de demanda por municipio a empresas de transporte estatales

**Funciones premium concretas que puedes construir:**
- `ChoferPro Badge`: verificación adicional, mejor posicionamiento en el radar del pasajero
- `Solicitud Urgente`: el pasajero paga más para aparecer primero en el radar
- `Reserva Programada Premium`: garantizar disponibilidad para viajes futuros con chofer asignado
- `Seguro de Viaje`: asociación con aseguradoras para cubrir el trayecto
- `API para Empresas`: flotas privadas que quieran usar la infraestructura

---

## SECCIÓN 9 — AUTOMATIZACIONES A IMPLEMENTAR

### Inmediatas (código + cron jobs)
1. **Limpieza de sesiones expiradas:** `DELETE FROM sesiones WHERE expira_en < NOW()` — job diario
2. **Invalidación de tokens FCM inválidos:** cuando FCM devuelve `messaging/registration-token-not-registered`, limpiar el token del usuario
3. **Estadísticas diarias:** job nocturno que pre-calcula y cachea métricas del dashboard admin
4. **Alerta de choferes pendientes:** si hay > X choferes esperando aprobación > 48h, notificar al admin por email

### A mediano plazo
5. **Sistema de reputación automático:** calcular y actualizar `calificacion_promedio` de choferes con trigger de PostgreSQL en lugar de calcularlo cada vez
6. **Detección de cancelaciones abusivas:** job que analiza patrones y suspende automáticamente choferes con > 5 cancelaciones en 24h
7. **Recordatorio de viaje programado:** notificación 2h antes de un `fecha_viaje`
8. **Reactivación de solicitudes expiradas sin oferta:** sugerir al pasajero re-publicar o ampliar el radio

---

## SECCIÓN 10 — MÉTRICAS Y LOGS QUE DEBES REGISTRAR

### Métricas de negocio (tabla `eventos` o solución como Mixpanel/Amplitude)
```sql
-- Eventos críticos a registrar
app_open, solicitud_creada, solicitud_vista_por_chofer,
oferta_enviada, oferta_aceptada, oferta_rechazada,
viaje_iniciado, viaje_completado, viaje_cancelado_pasajero,
viaje_cancelado_chofer, valoracion_dejada,
chofer_registrado, chofer_aprobado, chofer_rechazado,
usuario_registrado, session_created, session_revoked,
notificacion_enviada, notificacion_fallida
```

### Métricas de sistema (con Winston/Pino → Elasticsearch o Grafana)
- Tiempo de respuesta por endpoint (p50, p95, p99)
- Tasa de errores 4xx y 5xx por ruta
- Conexiones activas del pool de PostgreSQL
- Queries lentas (> 500ms)
- Fallos de FCM por tipo de error

### KPIs de producto que debes calcular semanalmente
- **Conversion rate:** solicitudes creadas → viajes completados
- **Time to first offer:** tiempo medio entre publicar y recibir primera oferta
- **Chofer retention:** choferes activos en semana N vs semana N-1
- **Cancellation rate** por tipo (pasajero vs chofer)
- **DAU/MAU ratio** (engagement)

---

## SECCIÓN 11 — ROADMAP RECOMENDADO

### Sprint 1 — Urgente: Blindar el producto (2 semanas)
- [ ] Revocar y rotar la API Key de Google Maps
- [ ] Activar HTTPS en el VPS (Nginx + Let's Encrypt)
- [ ] Corregir `isMinifyEnabled = true` y keystore de producción
- [ ] Fix CORS: eliminar el fallback a `'*'`
- [ ] Fix `getSolicitudById`: validar propiedad
- [ ] Fix `getPublicStats`: usar `Promise.all`
- [ ] Agregar índices a tabla `sesiones` (`token`, `expira_en`)
- [ ] Reemplazar `console.log` por Winston con niveles (error, warn, info, debug)

### Sprint 2 — Funcionalidad Crítica Faltante (2 semanas)
- [ ] Flujo de recuperación de contraseña (por email o código SMS)
- [ ] Timeout automático de viajes `en_proceso` sin finalizar (cron job separado del servidor)
- [ ] Calificación del pasajero por el chofer (bidireccional)
- [ ] Penalización y registro de cancelaciones del chofer
- [ ] Paginación en `getMisViajesChofer` y `getMisSolicitudes`
- [ ] Foto de perfil de usuario (migrar a Cloudinary)

### Sprint 3 — Experiencia y Retención (2-3 semanas)
- [ ] Chat básico in-app (WebSockets con Socket.io o Firebase Realtime DB)
- [ ] Seguimiento de ubicación del chofer en mapa
- [ ] Badge de notificaciones no leídas en la app
- [ ] Filtros avanzados en el radar (tipo de vehículo, interprovincial, precio)
- [ ] Sistema de reportes y quejas de usuarios

### Sprint 4 — Escalabilidad y Monetización (3-4 semanas)
- [ ] Migrar a Redis para caché de datos frecuentes
- [ ] Migrar cron jobs a workers independientes (BullMQ)
- [ ] Implementar suscripción para choferes (Plan Pro)
- [ ] Panel admin: mapa de solicitudes activas, exportación CSV
- [ ] Logging estructurado + métricas de negocio
- [ ] Suite de tests: al menos flujos de auth, solicitudes y ofertas

### Sprint 5 — Migración Técnica (en paralelo, sin urgencia)
- [ ] Migrar app Android de Java a Kotlin gradualmente
- [ ] Adoptar Jetpack Compose para nuevas pantallas
- [ ] Implementar arquitectura limpia (Repository + UseCase + ViewModel completo)
- [ ] Considerar Kotlin Multiplatform para compartir lógica con iOS futuro

---

## SECCIÓN 12 — DECISIONES TÉCNICAS QUE PUEDEN TRAER PROBLEMAS

| Decisión Actual | Riesgo en Producción | Solución |
|---|---|---|
| Imágenes en filesystem del VPS | Pérdida de datos si cae el servidor | Cloudinary / S3 |
| Cron dentro del servidor Express | Race conditions en multi-instancia | Worker proceso separado |
| HTTP sin SSL | Datos expuestos, pérdida de confianza | HTTPS obligatorio |
| Java sin Proguard | APK expuesto al reverse engineering | `isMinifyEnabled = true` |
| Sin caché | BD sobreexplotada en picos | Redis + cache-aside |
| Sin logs estructurados | Imposible diagnosticar errores en prod | Winston + niveles de log |
| Sin tests de integración | Regressions silenciosas | Jest/Vitest + supertest |
| Pool de 10 conexiones | Timeouts en picos de tráfico | Aumentar a 25-30, monitorear |
| FCM tokens en tabla usuarios | Desincronización multi-dispositivo | Tabla separada `dispositivos` |
| Rate limiting solo por IP | Evadible con proxies | Rate limiting por usuario autenticado |

---

## DIAGNÓSTICO FINAL

**Lo que está bien construido:**
- Transacciones de BD en flujos críticos (acepta oferta, finaliza viaje, registro)
- Sistema de sesiones multi-dispositivo con revocación
- Notificaciones FCM con batch y multicast
- Validación de datos de entrada con express-validator
- Estructura de proyecto clara y predecible
- Panel admin funcional con filtros por provincia
- Job de expiración de solicitudes (el concepto es correcto)
- MVVM parcialmente adoptado en Android
- Doble DB check + JWT para autenticación (defensa en profundidad)

**Lo que puede destruirte en producción:**
- API Key de Google Maps pública (te pueden cobrar miles)
- HTTP sin SSL (datos de usuarios expuestos)
- Cron dentro del servidor (problemas con multi-instancia)
- Sin recuperación de contraseña (usuarios atrapados)
- Sin timeout de viajes (datos corruptos)
- Sin proguard en release (APK reversible)
- Sin tests de integración (cambios que rompen cosas silenciosamente)

**La app tiene un núcleo sólido. No necesitas reescribir nada. Necesitas blindar lo que existe, completar los flujos rotos, y agregar la capa de experiencia que diferencia un producto serio de un MVP.**

---

*Auditoría generada el Mayo 2026. Para cualquier aclaración sobre prioridades de implementación, solicitar sesión de revisión técnica.*
