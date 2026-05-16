# 🚕 AUDITORÍA TÉCNICA Y ESTRATÉGICA COMPLETA — Te Busco App
**Fecha:** Mayo 2026 | **Evaluador:** Análisis a nivel CTO  
**Repositorio:** `https://github.com/roloramros/teBusco_app.git`

---

## RESUMEN EJECUTIVO

Te Busco es una aplicación de transporte colaborativo (modelo marketplace) para Cuba, compuesta por una app Android (Java), una API REST (Node.js/Express + PostgreSQL) y un panel de administración web (React). El proyecto demuestra una base técnica sólida para un MVP, con decisiones arquitectónicas razonables. Sin embargo, presenta **vulnerabilidades de seguridad críticas, problemas estructurales que bloquearán el crecimiento a escala, y carencias funcionales que lo alejan de ser un producto listo para producción.**

**Veredicto general: No está listo para lanzamiento masivo. Requiere de 4 a 8 semanas de trabajo enfocado antes de poder escalar de forma segura.**

---

## 1. VULNERABILIDADES DE SEGURIDAD — CRÍTICAS (Acción Inmediata)

### 🔴 1.1 API Key de Google Maps expuesta en el código fuente
**Severidad: CRÍTICA**

```java
// MainActivity.java — línea visible en repositorio público
Places.initialize(getApplicationContext(), "AIzaSyBufiSwuBW19JLsbXKDbW86pg_1wL7ifxU");

// GeocodingRepository.java
private static final String API_KEY = "AIzaSyBufiSwuBW19JLsbXKDbW86pg_1wL7ifxU";

// MapRepository.java
private static final String API_KEY = "AIzaSyBufiSwuBW19JLsbXKDbW86pg_1wL7ifxU";
```

Esta clave está expuesta en **cuatro lugares del código** y comprometida desde el momento en que el repositorio es público. Cualquier actor malicioso puede usarla para generar costos masivos de API sin restricción.

**Acción inmediata:**
1. Revocar la clave en Google Cloud Console ahora mismo.
2. Generar una nueva clave con restricciones: solo paquete Android (`com.codram.terecojo`) y APIs específicas (Maps SDK, Places, Directions).
3. Mover a `local.properties` o `gradle.properties` (no versionado), accedida via `BuildConfig`.
4. En el servidor, nunca exponer claves en código — usar variables de entorno.

---

### 🔴 1.2 Autenticación del Panel Admin insegura por diseño

```jsx
// AuthContext.jsx — El admin panel solo valida el tipo localmente
const login = async (identificador, password) => {
  const { data } = await api.post('/api/auth/login', { identificador, password });
  if (data.data.usuario.tipo !== 'admin') {
    throw new Error('Acceso denegado. Solo administradores.');
  }
  // ...
};
```

La validación de que el usuario es `admin` ocurre en el **cliente JavaScript**, no en el servidor. Un atacante puede simplemente ignorar esa comprobación en el frontend e intentar usar el token de cualquier usuario para llamar a rutas admin. Si bien las rutas de API sí validan con `authorize('admin')`, el panel frontend puede ser eludido trivialmente.

**Además**, el panel usa el mismo endpoint `/api/auth/login` que la app móvil, lo que significa que cualquier pasajero que obtuviera acceso al panel podría intentar login. Debería existir un endpoint o mecanismo de autenticación separado para el panel.

---


---

### 🔴 1.4 Sin límite de sesiones activas por usuario

El sistema crea una nueva sesión en la tabla `sesiones` en cada login sin limpiar sesiones antiguas del mismo usuario. Un usuario puede tener decenas de sesiones activas simultáneas. No hay mecanismo para que el usuario vea y revoque sus propias sesiones, ni control desde el admin panel.

---

### 🟡 1.5 CORS demasiado permisivo en producción

```javascript
origin: process.env.NODE_ENV === 'production'
  ? process.env.FRONTEND_URL || '*'
  : '*',
```

Si `FRONTEND_URL` no está configurada en producción, el CORS cae a `'*'` (permite cualquier origen). Esto es un error silencioso peligroso.

---

### 🟡 1.6 Imágenes de vehículos servidas localmente sin sanitización

Las fotos se guardan en disco del servidor (`/public/uploads/vehicles`). Problemas:
- Multer filtra por `mimetype` pero no valida el contenido real del archivo (magic bytes).
- No hay límite de archivos por chofer.
- Las URLs se construyen con `req.protocol + req.get('host')`, lo que puede generar URLs incorrectas detrás de un reverse proxy.
- En producción, archivos locales no sobreviven a deploys sin volúmenes persistentes.

---

### 🟡 1.7 FCM Tokens expuestos en respuestas de Admin

```javascript
// adminController.js — devuelve fcm_token a cualquier admin
const sql = `SELECT c.*, u.fcm_token, u.id as user_id ...`
```

Los FCM tokens son datos sensibles de dispositivos. Devolverlos en la API expone la capacidad de enviar notificaciones push directamente a dispositivos de usuarios sin pasar por el sistema.

---

## 2. PROBLEMAS ARQUITECTÓNICOS

### 🔴 2.1 Arquitectura de notificaciones sin escalabilidad: N+1 queries y bucles secuenciales

```javascript
// notificationService.js — bucle secuencial para cada chofer del municipio
for (const user of users) {
  await query(`INSERT INTO notificaciones ...`);
}

// solicitudController.js — bucle secuencial para notificar rechazados
for (const row of rejectedRows) {
  await sendNotification({ ... });
}
```

Cuando una solicitud se crea, el sistema hace N inserciones SQL secuenciales (una por chofer del municipio). Si hay 200 choferes en La Habana, esto genera 200 queries secuenciales bloqueando el request del pasajero por varios segundos. Además, los envíos FCM también son secuenciales.

**Solución:** Usar `INSERT ... SELECT` para insertar en batch, y `admin.messaging().sendEachForMulticast()` para push en batch.

---

### 🔴 2.2 Sin sistema de cola de trabajos (Job Queue)

Todas las notificaciones se envían de forma síncrona dentro del mismo request HTTP. Cualquier fallo en FCM o lentitud en la base de datos bloquea la respuesta al usuario. Para producción se necesita una cola (Bull/BullMQ con Redis) que procese notificaciones en background.

---

### 🔴 2.3 Singleton de Retrofit no thread-safe con contexto variable

```java
// RetrofitClient.java
private static Retrofit retrofit = null;
public static ApiService getService(Context context) {
  if (retrofit == null) { ... }
  return retrofit.create(ApiService.class);
}
```

`retrofit.create(ApiService.class)` se llama en cada invocación, lo cual crea una nueva instancia del proxy cada vez (costoso). El token se lee de `SessionManager` en cada request via el interceptor, lo cual es correcto, pero el singleton no está correctamente sincronizado para acceso concurrente (aunque en Android esto rara vez causa problemas prácticos, es un anti-pattern).

---

### 🟡 2.4 Pool de base de datos sin SSL y sin configuración de producción robusta

```javascript
const pool = new Pool({
  host: process.env.DB_HOST || 'localhost',
  // Sin SSL configurado
  max: 10,
});
```

No hay configuración SSL para la conexión a PostgreSQL. En un VPS donde la app y la DB corren juntas esto es aceptable, pero es un riesgo si algún día la DB migra a un servidor separado.

---

### 🟡 2.7 No hay soft-delete en solicitudes ni protección de datos históricos

Los vehículos se eliminan físicamente con `DELETE FROM vehiculos`. Si un viaje completado referencia ese vehículo, se pierde el historial. Se necesita soft-delete (`activo = false`) consistente en toda la aplicación.

---

## 3. PROBLEMAS DE LÓGICA DE NEGOCIO

### 🔴 3.1 Race condition en aceptación de ofertas

```javascript
// solicitudController.js — aceptarRespuesta
await client.query("UPDATE respuestas_solicitud SET estado = 'aceptado' WHERE id = $1", [respuesta_id])
await client.query("UPDATE solicitudes SET estado = 'en_proceso' WHERE id = $2", [oferta.solicitud_id])
```

Si dos pasajeros (o el mismo pasajero con doble click) intentan aceptar dos ofertas distintas de la misma solicitud simultáneamente, ambas transacciones podrían completarse antes de que la otra detecte el cambio. Se necesita `SELECT ... FOR UPDATE` o `UPDATE solicitudes SET estado='en_proceso' WHERE id=$1 AND estado='activa' RETURNING id` para garantizar atomicidad.

---

### 🔴 3.2 El sistema de calificación es unidireccional

Solo el pasajero califica al chofer. No existe calificación del chofer al pasajero. Esto es un problema para la confianza en la plataforma: los choferes no tienen forma de advertir sobre pasajeros problemáticos. Plataformas como Uber tienen calificación bidireccional.


---

### 🟡 3.4 Sin validación de que el chofer no oferte en su propia solicitud

Aunque los pasajeros y choferes son tipos distintos, un usuario podría técnicamente registrarse como ambos tipos en el futuro. No hay validación explícita de que `chofer_id != pasajero_id` en las ofertas.

---

### 🟡 3.6 Inconsistencia en tipos: `chofer_seleccionado_id` apunta a `choferes.id` pero el modelo está mezclado

En `solicitudes`, el campo `chofer_seleccionado_id` apunta al ID de la tabla `choferes`. Pero en `finalizarViaje`:
```javascript
// Este query es incorrecto — busca por choferes.usuario_id, no choferes.id
WHERE s.chofer_seleccionado_id = (SELECT usuario_id FROM choferes WHERE id = $1)
```
Hay confusión entre `usuarios.id`, `choferes.id` y `choferes.usuario_id` a lo largo de los controladores. Esto genera queries complejas y propensas a bugs.

---

## 4. ERRORES DE DISEÑO Y RENDIMIENTO EN ANDROID

### 🔴 4.1 Llamadas de red en el hilo principal (ausencia de repositorios completos)

Partes del código Android hacen llamadas Retrofit directamente desde Activities sin ViewModels:
```java
// DriverActivity.java — patrón detectado
RetrofitClient.getService(context).getVehicles().enqueue(...)
```
Las Activities no deberían hacer network calls directamente. El patrón MVVM está parcialmente implementado (existen ViewModels) pero no se usa de forma consistente.


---

### 🟡 4.3 Sin paginación en el radar de solicitudes del chofer

```javascript
// solicitudController.js — getTodasSolicitudesActivas
SELECT v.* FROM v_solicitudes v WHERE v.estado = 'activa' ORDER BY v.creada_en DESC
// Sin LIMIT ni OFFSET
```

Con 500 solicitudes activas simultáneas, esto devuelve todo al cliente. El RecyclerView del chofer cargará todos los datos de golpe. Se necesita paginación o lazy loading.

---

### 🟡 4.4 Polling manual implícito vs WebSockets/SSE

El chofer necesita ver nuevas solicitudes en tiempo real. La arquitectura actual depende enteramente de notificaciones push FCM, lo cual es frágil (FCM no es garantizado, Doze mode en Android). No hay mecanismo de polling periódico ni WebSockets. Si el FCM falla, el chofer no ve nuevas solicitudes hasta que refresca manualmente.


---

## 5. PROBLEMAS DE EXPERIENCIA DE USUARIO (UX/UI)

### 🔴 5.1 Flujo de onboarding inexistente

Un nuevo usuario que descarga la app llega directamente a la pantalla de login/registro sin ninguna explicación del servicio. No hay pantallas de onboarding, no hay demo del flujo, no hay screenshots del funcionamiento. La tasa de abandono en este punto será muy alta.

---


### 🟡 5.3 Sin confirmación visual del estado del viaje en tiempo real

Después de que el pasajero acepta una oferta, no hay un tracking visual del estado: "Chofer en camino → Chofer llegó → Viaje en curso". El sistema está diseñado como marketplace de solicitudes, no como tracker de viaje, lo cual es aceptable para el MVP, pero debe comunicarse claramente al usuario.

---

### 🟡 5.4 Panel admin sin gráficas de tendencia temporal

El dashboard admin muestra contadores pero sin tendencias temporales (registros por semana, viajes por día). Recharts está incluido en el package.json pero no hay ningún componente de gráfica implementado. Los datos están disponibles pero no visualizados.

---

### 🟡 5.5 Sin búsqueda ni filtros en historial del pasajero

`getMisSolicitudes` devuelve todo el historial sin paginación ni filtros. Un usuario con 50 viajes no puede buscar por fecha o estado.

---

### 🟡 5.6 Sin foto de perfil para usuarios/choferes

Existe el campo `foto_url` en la tabla de usuarios pero no hay endpoint ni UI para subir foto de perfil. Los choferes pueden subir fotos de vehículos pero no una foto propia, lo que reduce la confianza del pasajero.

---

## 6. PROBLEMAS DE MANTENIBILIDAD Y CÓDIGO

---

### 🟡 6.3 Cero tests automatizados funcionales

Existe `test/stats.test.js` pero está vacío o es mínimo. No hay tests de integración para los flujos críticos: crear solicitud, hacer oferta, aceptar oferta, finalizar viaje. Un bug en `aceptarRespuesta` podría pasar desapercibido hasta producción.

---

### 🟡 6.4 Sin documentación de la API (Swagger/OpenAPI)

No existe documentación de los endpoints. Cualquier desarrollador nuevo (o integración futura con otras apps) tendrá que leer el código para entender qué parámetros acepta cada endpoint.

---

### 🟡 6.5 Nombre inconsistente del proyecto

El paquete Android es `com.codram.terecojo`, los archivos del backend dicen `tebusco-api`, el nombre de la app es "Te Busco" pero el directorio es `teBusco_app`. Esta inconsistencia complica el branding y sugiere que el nombre cambió durante el desarrollo sin una refactorización completa.

---

### 🟡 6.6 Sin archivo `.env.example`

No existe un `.env.example` en el repositorio. Un nuevo desarrollador que clone el repo no sabe qué variables de entorno configurar.

---

## 7. FUNCIONES FALTANTES CRÍTICAS

### Para considerarse un producto profesional y completo, faltan:

**Seguridad y Confianza:**
- Verificación de número de teléfono por SMS/WhatsApp antes de activar cuenta
- Sistema de reporte de usuarios (pasajero reporta chofer y viceversa)
- Validación de documentos del chofer (foto de licencia, foto de carnet) con revisión admin
- Historial de cambios de estado visible para el usuario

**Funcionalidad Core:**
- Sistema de chat en tiempo real entre pasajero y chofer (una vez aceptada la oferta)
- Calificación del pasajero por el chofer
- Perfil público del chofer con historial de valoraciones y foto
- Historial completo de viajes con recibo/detalle descargable
- Posibilidad de editar solicitud antes de recibir ofertas
- Cancelación con penalización configurable

**Operación:**
- Job de limpieza de sesiones expiradas (`DELETE FROM sesiones WHERE expira_en < NOW()`)
- Job de expiración automática de solicitudes antiguas
- Job de actualización de `calificacion_promedio` en choferes (actualmente si no hay trigger en DB, el promedio no se actualiza automáticamente)
- Sistema de métricas operacionales (tiempo de respuesta de API, tasa de error, etc.)

**Admin Panel:**
- Gráficas de tendencia (registros por día/semana, viajes completados vs cancelados)
- Exportación de datos a CSV
- Gestión de provincias/municipios (activar/desactivar)
- Log de acciones del admin
- Sistema de configuración de la app desde el panel (textos, límites, features flags)

---

## 8. ESCALABILIDAD — CAMINO A MILES DE USUARIOS

### Situación actual: Capacidad estimada ~500-1,000 usuarios concurrentes en un VPS simple

### Para llegar a 10,000+ usuarios:

**Nivel 1 (0-5,000 usuarios):** Un VPS con 4-8 GB RAM es suficiente si:
- Se implementan los índices de DB faltantes
- Se agrega caché para datos estáticos (geo)
- Las notificaciones se procesan en background (Bull Queue)
- Se agrega una sesión de limpieza programada

**Nivel 2 (5,000-50,000 usuarios):**
- Migrar a una arquitectura con separación DB-App (PostgreSQL en servidor dedicado)
- Implementar Redis para caché y colas de trabajo
- Agregar CDN para imágenes (Cloudflare R2, AWS S3)
- Considerar read replicas para la DB
- Monitoring con Prometheus/Grafana o similar

**Nivel 3 (50,000+ usuarios):**
- Microservicios (separar servicio de notificaciones, servicio geo, servicio de viajes)
- Load balancer con múltiples instancias de la API
- Event-driven architecture para notificaciones

---

## 9. MONETIZACIÓN — OPORTUNIDADES NO EXPLOTADAS

El proyecto tiene **cero infraestructura de monetización** actualmente. Opciones recomendadas ordenadas por facilidad de implementación:

**Inmediatas (1-2 semanas):**
- **Comisión por viaje completado:** Cobrar X% del precio del viaje. Requiere integrar pasarela de pago (transferencia bancaria + confirmación manual en MVP)
- **Suscripción de chofer "Pro":** Acceso prioritario al radar, badge de verificado premium, estadísticas avanzadas

**Corto plazo (1-2 meses):**
- **Boost de solicitud:** El pasajero paga para que su solicitud aparezca primero en el radar de choferes
- **Publicidad local:** Mostrar anuncios de negocios locales en las pantallas de espera
- **Datos agregados:** Vender reportes de movilidad anonimizados a municipios o investigadores

**Mediano plazo:**
- **Viajes programados premium:** Garantizar disponibilidad de chofer con antelación
- **Seguro de viaje:** Integración con aseguradoras

---

## 10. INTEGRACIONES FUTURAS RECOMENDADAS

- **WhatsApp Business API:** Notificaciones por WhatsApp como alternativa/complemento a FCM (más confiable en Cuba)
- **Mapas offline:** Implementar tiles de OpenStreetMap para funcionar sin conexión
- **Pasarela de pagos:** Enzona, Transfermóvil o MLC cuando sea técnicamente posible
- **Analytics:** Mixpanel o PostHog para entender comportamiento de usuarios
- **Crashlytics (Firebase):** Ya tienen Firebase, solo falta agregar el módulo de crashes

---

## 11. MÉTRICAS Y LOGS QUE DEBES ALMACENAR

**Tabla de eventos de negocio (a crear):**
```sql
CREATE TABLE eventos (
  id BIGSERIAL PRIMARY KEY,
  tipo VARCHAR(100) NOT NULL,        -- 'solicitud_creada', 'oferta_aceptada', etc.
  usuario_id INTEGER REFERENCES usuarios,
  datos JSONB,
  creado_en TIMESTAMPTZ DEFAULT NOW()
);
```

**KPIs que debes trackear:**
- Tiempo promedio entre solicitud y primera oferta
- Tasa de conversión: solicitudes que reciben al menos 1 oferta
- Tasa de completación: ofertas aceptadas que llegan a `completada`
- Churn de choferes: choferes que se registran pero nunca hacen una oferta
- Tasa de cancelación (pasajero vs chofer)
- Tiempo promedio de verificación de chofer por admin

---

## 12. ROADMAP TÉCNICO RECOMENDADO

### Semana 1-2: SEGURIDAD (No negociable antes de lanzar)
1. Revocar y regenerar API Key de Google Maps con restricciones
2. Eliminar fallback del JWT_SECRET (fallo explícito si no está definida)
3. Cambiar nivel de logging HTTP a NONE en release builds
4. Agregar `.env.example` al repositorio
5. Separar login del admin panel del endpoint principal

### Semana 3-4: ESTABILIDAD
6. Agregar índices de base de datos críticos
7. Agregar caché para endpoints geo
8. Implementar `SELECT FOR UPDATE` en `aceptarRespuesta`
9. Agregar paginación a todos los endpoints de listado
10. Job cron para limpieza de sesiones y expiración de solicitudes

### Semana 5-6: UX/PRODUCTO
11. Pantallas de onboarding (3-4 slides)
12. Estados vacíos ilustrados
13. Sistema de calificación bidireccional
14. Foto de perfil para choferes
15. Perfil público del chofer

### Semana 7-8: MONITOREO Y ESCALABILIDAD
16. Logging estructurado con Winston/Pino
17. Tests de integración para flujos críticos
18. Documentación de API (Swagger)
19. Migración de imágenes a almacenamiento externo (S3/R2)
20. Bull Queue para notificaciones en background

### Semana 9-12: MONETIZACIÓN
21. Sistema de comisión por viaje
22. Plan "Chofer Pro"
23. Dashboard de analytics para el negocio

---

## 13. LO QUE ESTÁ BIEN — RECONOCIMIENTO JUSTO

No todo es negativo. El proyecto tiene decisiones correctas que merecen reconocimiento:

- **Rate limiting bien implementado:** Global y específico para auth, con mensajes claros.
- **Transacciones de base de datos correctas:** `BEGIN/COMMIT/ROLLBACK` donde se necesita.
- **Separación de responsabilidades razonable:** Controladores, servicios, middleware bien divididos.
- **Helmet y CORS configurados:** Cabeceras de seguridad básicas presentes.
- **Mensajes de error genéricos en auth:** No revelan si el usuario existe.
- **Express-validator con reglas sensatas:** Validación de entrada implementada.
- **FCM Topics por municipio:** Arquitectura inteligente para notificar choferes locales.
- **Documentación de planes/specs en `/docs`:** Evidencia de un proceso de desarrollo reflexivo.
- **Sistema de sesiones en BD:** Permite invalidación real de tokens, mejor que JWT puro.
- **MVVM parcialmente implementado en Android:** LiveData y ViewModels presentes.

---

## CONCLUSIÓN DEL CTO

Te Busco es un proyecto con una visión de negocio clara y una implementación que supera el nivel de un proyecto personal. Sin embargo, tiene **tres problemas que deben resolverse antes de cualquier lanzamiento público:**

1. **La API Key de Google Maps está comprometida y debe revocarse hoy.**
2. **El secreto JWT tiene un fallback inseguro que puede permitir forjar tokens.**
3. **El sistema de notificaciones es síncrono y secuencial, lo que causará timeouts bajo carga.**

El resto de los problemas son deuda técnica que puede pagarse progresivamente, pero los tres anteriores son riesgos activos de seguridad y estabilidad.

Con 6-8 semanas de trabajo enfocado en los puntos de este roadmap, el proyecto puede estar listo para un lanzamiento beta controlado con hasta 2,000 usuarios. La arquitectura base permite crecer, pero necesita los cimientos de seguridad y estabilidad antes de escalar.

---
*Auditoría generada con análisis completo del código fuente. Mayo 2026.*
