# Intelligent Notification Navigation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement intelligent navigation in the Android app based on the `tipo` field received in FCM notification payloads, ensuring users are directed to the correct screen when tapping a notification.

**Architecture:** Update `MyFirebaseMessagingService` to extract metadata from the FCM `data` payload and use a centralized routing method (`resolveDestinationIntent`) to determine the target Activity. Use unique `requestCode` and `notificationId` based on the notification type to prevent Intent collisions and notification overwriting.

**Tech Stack:** Java (Android), Firebase Cloud Messaging (FCM).

---

### Task 1: Update Imports and Constants in MyFirebaseMessagingService

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/utils/MyFirebaseMessagingService.java`

- [ ] **Step 1: Update imports**
Add necessary imports for `Map`, `AuthResponse`, and destination Activities.

```java
import com.codram.terecojo.DriverActivity;
import com.codram.terecojo.DriverTripsActivity;
import com.codram.terecojo.MyRequestsActivity;
import com.codram.terecojo.NotificationsActivity;
import com.codram.terecojo.data.model.AuthResponse;
import java.util.Map;
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/codram/terecojo/utils/MyFirebaseMessagingService.java
git commit -m "chore: add imports to MyFirebaseMessagingService"
```

---

### Task 2: Implement resolveDestinationIntent in MyFirebaseMessagingService

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/utils/MyFirebaseMessagingService.java`

- [ ] **Step 1: Add resolveDestinationIntent method**
Implement the routing logic based on the `tipo` string and user role.

```java
    private Intent resolveDestinationIntent(String tipo) {
        Class<?> destination;

        if (tipo == null) {
            destination = NotificationsActivity.class;
        } else {
            switch (tipo) {
                case "nueva_solicitud":
                    destination = DriverActivity.class;
                    break;
                case "nueva_oferta":
                    destination = MyRequestsActivity.class;
                    break;
                case "oferta_aceptada":
                    destination = DriverTripsActivity.class;
                    break;
                case "oferta_rechazada":
                    destination = DriverActivity.class;
                    break;
                case "viaje_cancelado":
                    AuthResponse.User user = SessionManager.getInstance(this).getUser();
                    if (user != null && "chofer".equalsIgnoreCase(user.getTipo())) {
                        destination = DriverTripsActivity.class;
                    } else {
                        destination = MyRequestsActivity.class;
                    }
                    break;
                case "viaje_completado":
                    destination = DriverTripsActivity.class;
                    break;
                case "sistema_alerta":
                default:
                    destination = NotificationsActivity.class;
                    break;
            }
        }

        return new Intent(this, destination);
    }
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/codram/terecojo/utils/MyFirebaseMessagingService.java
git commit -m "feat: implement resolveDestinationIntent in MyFirebaseMessagingService"
```

---

### Task 3: Update sendNotification in MyFirebaseMessagingService

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/utils/MyFirebaseMessagingService.java`

- [ ] **Step 1: Update method signature and implementation**
Change `sendNotification(String title, String messageBody)` to `sendNotification(String title, String messageBody, String tipo, String solicitudId)`.

```java
    private void sendNotification(String title, String messageBody, String tipo, String solicitudId) {
        Intent intent = resolveDestinationIntent(tipo);

        if (solicitudId != null && !solicitudId.isEmpty()) {
            intent.putExtra("solicitud_id", solicitudId);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int requestCode = tipo != null ? tipo.hashCode() & 0xFFFF : 0;

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String channelId = "default_channel_id";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_notifications)
                        .setContentTitle(title != null ? title : "Te Busco")
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Canal de Notificaciones Te Busco",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        int notificationId = tipo != null ? tipo.hashCode() & 0xFF : 0;
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/codram/terecojo/utils/MyFirebaseMessagingService.java
git commit -m "feat: update sendNotification to support routing and unique IDs"
```

---

### Task 4: Update onMessageReceived in MyFirebaseMessagingService

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/utils/MyFirebaseMessagingService.java`

- [ ] **Step 1: Update onMessageReceived to extract data payload**
Modify the method to extract `tipo` and `solicitud_id` and pass them to `sendNotification`.

```java
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (SessionManager.getInstance(this).getToken() == null) {
            Log.d(TAG, "Mensaje ignorado: No hay sesión activa.");
            return;
        }

        Map<String, String> data = remoteMessage.getData();
        String tipo        = data.getOrDefault("tipo", null);
        String solicitudId = data.getOrDefault("solicitud_id", null);

        Log.d(TAG, "FCM recibido — tipo: " + tipo + ", solicitud_id: " + solicitudId);

        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body  = remoteMessage.getNotification().getBody();
            sendNotification(title, body, tipo, solicitudId);
        } else if (!data.isEmpty()) {
            String title = data.getOrDefault("titulo", "Te Busco");
            String body  = data.getOrDefault("cuerpo", "Tienes una nueva notificación");
            sendNotification(title, body, tipo, solicitudId);
        }
    }
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/codram/terecojo/utils/MyFirebaseMessagingService.java
git commit -m "feat: update onMessageReceived to handle data payload and call updated sendNotification"
```

---

### Task 5: Fix navigation bug in NotificationsActivity

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/NotificationsActivity.java`

- [ ] **Step 1: Update navigateBasedOnNotification method**
Change destination for `nueva_solicitud` from `DriverOffersActivity` to `DriverActivity`.

```java
    private void navigateBasedOnNotification(Notification notification) {
        Intent intent = null;
        switch (notification.getTipo()) {
            case "nueva_oferta":
                intent = new Intent(this, MyRequestsActivity.class);
                break;
            case "nueva_solicitud":
                intent = new Intent(this, DriverActivity.class); // Fix: DriverActivity instead of DriverOffersActivity
                break;
            case "oferta_aceptada":
            case "viaje_confirmado":
                intent = new Intent(this, MyRequestsActivity.class);
                break;
        }

        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
    }
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/codram/terecojo/NotificationsActivity.java
git commit -m "fix: correct navigation for nueva_solicitud in NotificationsActivity"
```
