# Migración de Notificaciones y Radar a Nivel de Provincia

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ampliar la visibilidad de solicitudes y las notificaciones push para los choferes, pasando de un alcance municipal a provincial.

**Architecture:** Se modificará la lógica de filtrado en el radar de choferes para usar `provincia_base_id`, se actualizará la creación de solicitudes para emitir notificaciones a tópicos de provincia, y se ajustará el servicio de persistencia de notificaciones. En Android, se actualizarán los procesos de suscripción y desuscripción de tópicos de FCM.

**Tech Stack:** Node.js, Express, PostgreSQL, Java (Android), Firebase Cloud Messaging (FCM).

---

### Task 1: Actualizar Radar de Choferes (API)

**Files:**
- Modify: `tebusco-api/src/controllers/solicitudController.js`

- [ ] **Step 1: Modificar `getTodasSolicitudesActivas` para filtrar por provincia**

```javascript
// Localizar la función getTodasSolicitudesActivas
// Cambiar la obtención del perfil del chofer para incluir provincia_base_id
// y agregar el filtro WHERE origen_provincia_id = $2

export const getTodasSolicitudesActivas = async (req, res, next) => {
  try {
    const { id: usuarioId } = req.usuario;

    // 1. Obtener el ID de chofer y su provincia base
    const { rows: choferRows } = await query(
      'SELECT id, provincia_base_id FROM choferes WHERE usuario_id = $1',
      [usuarioId]
    )
    
    if (choferRows.length === 0) {
      return success(res, []) // O manejar como error si se prefiere
    }
    
    const { id: choferId, provincia_base_id } = choferRows[0];
    
    if (!provincia_base_id) {
      return success(res, [])
    }

    let sql = `
      SELECT v.*, 
        CASE WHEN r.id IS NOT NULL THEN TRUE ELSE FALSE END as ha_respondido
      FROM v_solicitudes v
      JOIN solicitudes s ON s.id = v.id
      LEFT JOIN respuestas_solicitud r ON r.solicitud_id = v.id AND r.chofer_id = $1 AND r.estado != 'rechazado'
      WHERE v.estado = 'activa' AND v.origen_provincia_id = $2
    `;
    let params = [choferId, provincia_base_id];

    sql += ` ORDER BY v.creada_en DESC`;

    const { rows } = await query(sql, params)
    return success(res, rows)
  } catch (err) {
    next(err)
  }
}
```

- [ ] **Step 2: Commit cambios del radar**

```bash
git add tebusco-api/src/controllers/solicitudController.js
git commit -m "feat(api): update radar to filter by province"
```

### Task 2: Actualizar Notificación de Nueva Solicitud (API)

**Files:**
- Modify: `tebusco-api/src/controllers/solicitudController.js`

- [ ] **Step 1: Modificar `createSolicitud` para usar tópico de provincia**

```javascript
// Localizar el bloque de notificación en createSolicitud
// Cambiar municipio_${target_municipio_id} por provincia_${resolved_origen_provincia_id}

    // ... (dentro de createSolicitud)
    const nuevaSolicitud = rows[0]

    // ─────────────────────────────────────────────────────────
    // NOTIFICACIÓN (Choferes de la provincia)
    // ─────────────────────────────────────────────────────────
    if (process.env.NODE_ENV !== 'development') {
      const target_provincia_id = resolved_origen_provincia_id;
      if (target_provincia_id) {
        await sendNotification({
          usuario_id: null,
          tipo: 'nueva_solicitud',
          titulo: '🚕 ¡Nueva solicitud de viaje!',
          cuerpo: `${pasajeroNombre} busca viaje desde ${origen_descripcion} hasta ${destino_descripcion}`,
          datos_extra: { solicitud_id: nuevaSolicitud.id.toString() },
          topic: `provincia_${target_provincia_id}`
        });
      }
    }
```

- [ ] **Step 2: Commit cambios de creación**

```bash
git add tebusco-api/src/controllers/solicitudController.js
git commit -m "feat(api): update notification topic to province in createSolicitud"
```

### Task 3: Actualizar Persistencia de Notificaciones (API)

**Files:**
- Modify: `tebusco-api/src/services/notificationService.js`

- [ ] **Step 1: Modificar persistencia para manejar tópicos de provincia**

```javascript
// Localizar el bloque que maneja topics en sendNotification
// Cambiar la lógica de municipio_ a provincia_

    // ... (dentro de sendNotification)
    if (topic && topic.startsWith('provincia_')) {
      const provinciaId = topic.replace('provincia_', '');
      try {
        const { rows: users } = await query(
          `SELECT usuario_id FROM choferes WHERE provincia_base_id = $1`,
          [provinciaId]
        );
        // ... rest of logic to insert into notifications table for each user
```

- [ ] **Step 2: Commit cambios de persistencia**

```bash
git add tebusco-api/src/services/notificationService.js
git commit -m "feat(api): update notification persistence to handle province topics"
```

### Task 4: Actualizar Suscripción en Login (Android)

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/LoginActivity.java`

- [ ] **Step 1: Cambiar suscripción de municipio a provincia**

```java
// Localizar el bloque en executeLogin (línea ~155)
if (auth.getUser().getTipo().equalsIgnoreCase("chofer") && auth.getUser().getProvincia_id() != null) {
    String topic = "provincia_" + auth.getUser().getProvincia_id();
    FirebaseMessaging.getInstance().subscribeToTopic(topic);
    Log.d("FCM", "Suscrito al tema: " + topic);
}
```

- [ ] **Step 2: Commit cambios Login**

```bash
git add app/src/main/java/com/codram/terecojo/LoginActivity.java
git commit -m "feat(android): update FCM subscription to province on login"
```

### Task 5: Actualizar Desuscripción en Logout (Android)

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/utils/SessionManager.java`

- [ ] **Step 1: Cambiar desuscripción de municipio a provincia**

```java
// Localizar el método logout (línea ~76)
AuthResponse.User user = getUser();
if (user != null && "chofer".equalsIgnoreCase(user.getTipo()) && user.getProvincia_id() != null) {
    String topic = "provincia_" + user.getProvincia_id();
    com.google.firebase.messaging.FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("FCM", "Desuscrito exitosamente de: " + topic);
                }
            });
}
```

- [ ] **Step 2: Commit cambios SessionManager**

```bash
git add app/src/main/java/com/codram/terecojo/utils/SessionManager.java
git commit -m "feat(android): update FCM unsubscription to province on logout"
```

### Task 6: Asegurar Suscripción en DriverActivity (Android)

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/DriverActivity.java`

- [ ] **Step 1: Agregar suscripción proactiva en `onCreate`**

```java
// Después de viewModel.fetchMyVehicles(); en onCreate
viewModel.fetchMyVehicles();

// Nueva lógica de suscripción proactiva
AuthResponse.User user = SessionManager.getInstance(this).getUser();
if (user != null && "chofer".equalsIgnoreCase(user.getTipo()) && user.getProvincia_id() != null) {
    String topic = "provincia_" + user.getProvincia_id();
    com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic(topic);
    Log.d("FCM", "Suscripción proactiva al tema: " + topic);
}
```

- [ ] **Step 2: Commit cambios DriverActivity**

```bash
git add app/src/main/java/com/codram/terecojo/DriverActivity.java
git commit -m "feat(android): add proactive FCM subscription in DriverActivity"
```
