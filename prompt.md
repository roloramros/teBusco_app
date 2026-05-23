# PROMPT — Implementar navegación inteligente por tipo de notificación FCM
**Proyecto:** Te Busco App (Android Java + Node.js/Express)  
**Archivo principal a modificar:** `app/src/main/java/com/codram/terecojo/utils/MyFirebaseMessagingService.java`  
**Archivos de referencia (no modificar, solo leer):**
- `app/src/main/java/com/codram/terecojo/data/model/Notification.java`
- `app/src/main/java/com/codram/terecojo/NotificationsActivity.java` → método `navigateBasedOnNotification()`
- `tebusco-api/src/services/notificationService.js`
- `tebusco-api/src/controllers/solicitudController.js`
---
 
## CONTEXTO DEL PROBLEMA
 
### Comportamiento actual (incorrecto)
Cuando el servidor envía una notificación push FCM, el `MyFirebaseMessagingService` la recibe en `onMessageReceived()`. El método `sendNotification()` construye la notificación del sistema y le asigna **siempre** el mismo destino al hacer tap:
 
```java
// CÓDIGO ACTUAL — MyFirebaseMessagingService.java
private void sendNotification(String title, String messageBody) {
    Intent intent = new Intent(this, MainActivity.class); // ← HARDCODEADO, SIEMPRE MainActivity
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
    // ... build y show de la notificación
}
```
 
### Comportamiento esperado
El payload FCM que envía el servidor **ya incluye** un campo `tipo` y un `solicitud_id` en el bloque `data`. La app debe leer esos datos y construir un `Intent` que lleve al usuario exactamente a la pantalla correcta según el tipo de notificación.
 
---
 
## DATOS QUE LLEGAN DEL SERVIDOR (YA IMPLEMENTADOS — NO CAMBIAR EL BACKEND)
 
El servidor envía cada push con este formato:
 
```json
{
  "notification": {
    "title": "Nueva oferta recibida",
    "body": "Un chofer ha ofertado tu viaje"
  },
  "data": {
    "tipo": "nueva_oferta",
    "solicitud_id": "42",
    "notificacion_id": "128"
  }
}
```
 
El campo `data` llega en Android como `remoteMessage.getData()`, que devuelve un `Map<String, String>`.
 
### Todos los tipos de notificación que emite el backend
 
| `tipo` (String) | Quién lo recibe | Pantalla destino correcta |
|---|---|---|
| `nueva_solicitud` | Chofer | `DriverActivity.class` |
| `nueva_oferta` | Pasajero | `MyRequestsActivity.class` |
| `oferta_aceptada` | Chofer | `DriverTripsActivity.class` |
| `oferta_rechazada` | Chofer | `DriverActivity.class` (volver al radar) |
| `viaje_cancelado` | Pasajero o Chofer | Depende del rol del usuario en sesión: si es pasajero → `MyRequestsActivity.class`; si es chofer → `DriverTripsActivity.class` |
| `viaje_completado` | Chofer | `DriverTripsActivity.class` |
| `sistema_alerta` | Admin/Sistema | `NotificationsActivity.class` |
| `null` o desconocido | Cualquiera | `NotificationsActivity.class` (fallback seguro) |
 
### Cómo obtener el rol del usuario en sesión (ya existe en el proyecto)
 
```java
// SessionManager ya existe — úsalo así:
AuthResponse.User user = SessionManager.getInstance(this).getUser();
String tipoUsuario = user != null ? user.getTipo() : null; // "pasajero", "chofer", "admin"
```
 
---
 
## CAMBIOS A IMPLEMENTAR
 
### Archivo: `MyFirebaseMessagingService.java`
 
#### Cambio 1 — Modificar `onMessageReceived()` para extraer el `data` payload
 
**Situación actual:** `onMessageReceived()` extrae título y body de `remoteMessage.getNotification()` y llama a `sendNotification(title, body)`. El `data` payload se loguea pero se descarta.
 
**Situación requerida:** Extraer también `tipo` y `solicitud_id` del `data` payload y pasarlos a `sendNotification()`.
 
```java
// ANTES
@Override
public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
    // ...
    if (remoteMessage.getNotification() != null) {
        String title = remoteMessage.getNotification().getTitle();
        String body  = remoteMessage.getNotification().getBody();
        sendNotification(title, body);  // ← solo 2 parámetros
    }
}
 
// DESPUÉS
@Override
public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
    if (SessionManager.getInstance(this).getToken() == null) {
        Log.d(TAG, "Mensaje ignorado: No hay sesión activa.");
        return;
    }
 
    // Extraer data payload (siempre presente aunque notification sea nulo)
    Map<String, String> data = remoteMessage.getData();
    String tipo         = data.getOrDefault("tipo", null);
    String solicitudId  = data.getOrDefault("solicitud_id", null);
 
    Log.d(TAG, "FCM recibido — tipo: " + tipo + ", solicitud_id: " + solicitudId);
 
    if (remoteMessage.getNotification() != null) {
        String title = remoteMessage.getNotification().getTitle();
        String body  = remoteMessage.getNotification().getBody();
        sendNotification(title, body, tipo, solicitudId);  // ← 4 parámetros
    } else if (!data.isEmpty()) {
        // Notificación data-only (sin bloque notification) — construir desde data
        String title = data.getOrDefault("titulo", "Te Busco");
        String body  = data.getOrDefault("cuerpo", "Tienes una nueva notificación");
        sendNotification(title, body, tipo, solicitudId);
    }
}
```
 
> **Nota:** El import que necesitas agregar es `java.util.Map` — los demás ya están en el archivo.
 
---
 
#### Cambio 2 — Reemplazar `sendNotification(String, String)` por `sendNotification(String, String, String, String)`
 
Este es el cambio central. El método debe construir un `Intent` distinto según el valor de `tipo`.
 
```java
// MÉTODO COMPLETO A REEMPLAZAR
private void sendNotification(String title, String messageBody, String tipo, String solicitudId) {
 
    // 1. Determinar la pantalla destino según el tipo de notificación
    Intent intent = resolveDestinationIntent(tipo);
 
    // 2. Si hay solicitud_id, pasarlo como extra para que la pantalla destino
    //    pueda pre-seleccionar o destacar el viaje relevante (útil en el futuro)
    if (solicitudId != null && !solicitudId.isEmpty()) {
        intent.putExtra("solicitud_id", solicitudId);
    }
 
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
 
    // 3. Usar requestCode único basado en el tipo para evitar colisión de PendingIntents
    //    Si todos usan requestCode=0, Android puede reutilizar un PendingIntent viejo
    //    con el Intent incorrecto cuando llegan dos notificaciones distintas seguidas.
    int requestCode = tipo != null ? tipo.hashCode() & 0xFFFF : 0;
 
    PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );
 
    // 4. Construir y mostrar la notificación del sistema (igual que antes)
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
 
    // 5. Usar un notificationId único por tipo para que múltiples notificaciones
    //    del mismo tipo se apilen en lugar de reemplazarse entre sí con IDs diferentes.
    int notificationId = tipo != null ? tipo.hashCode() & 0xFF : 0;
    notificationManager.notify(notificationId, notificationBuilder.build());
}
```
 
---
 
#### Cambio 3 — Agregar el método privado `resolveDestinationIntent()`
 
Este método centraliza toda la lógica de routing. Debe agregarse como método privado nuevo en la clase.
 
```java
private Intent resolveDestinationIntent(String tipo) {
    Class<?> destination;
 
    if (tipo == null) {
        // Sin tipo conocido: ir a la bandeja de notificaciones
        destination = com.codram.terecojo.NotificationsActivity.class;
 
    } else {
        switch (tipo) {
 
            case "nueva_solicitud":
                // El chofer recibe esto → lo llevamos al radar
                destination = com.codram.terecojo.DriverActivity.class;
                break;
 
            case "nueva_oferta":
                // El pasajero recibe esto → ver sus solicitudes y las ofertas
                destination = com.codram.terecojo.MyRequestsActivity.class;
                break;
 
            case "oferta_aceptada":
                // El chofer recibe esto → ver sus viajes confirmados
                destination = com.codram.terecojo.DriverTripsActivity.class;
                break;
 
            case "oferta_rechazada":
                // El chofer recibe esto → volver al radar a buscar otro viaje
                destination = com.codram.terecojo.DriverActivity.class;
                break;
 
            case "viaje_cancelado":
                // Puede recibirlo tanto el pasajero como el chofer.
                // Leer el rol del usuario en sesión para decidir.
                AuthResponse.User user = SessionManager.getInstance(this).getUser();
                if (user != null && "chofer".equalsIgnoreCase(user.getTipo())) {
                    destination = com.codram.terecojo.DriverTripsActivity.class;
                } else {
                    destination = com.codram.terecojo.MyRequestsActivity.class;
                }
                break;
 
            case "viaje_completado":
                // El chofer recibe esto → ver historial de viajes
                destination = com.codram.terecojo.DriverTripsActivity.class;
                break;
 
            case "sistema_alerta":
            default:
                // Sistema, admin, o tipo desconocido → bandeja de notificaciones
                destination = com.codram.terecojo.NotificationsActivity.class;
                break;
        }
    }
 
    return new Intent(this, destination);
}
```
 
> **Imports que necesita este método** — agregar al bloque de imports de la clase si no están ya:
> ```java
> import com.codram.terecojo.DriverActivity;
> import com.codram.terecojo.DriverTripsActivity;
> import com.codram.terecojo.MyRequestsActivity;
> import com.codram.terecojo.NotificationsActivity;
> import com.codram.terecojo.data.model.AuthResponse;
> ```
 
---
 
## RESULTADO FINAL — Cómo debe quedar `MyFirebaseMessagingService.java` completo
 
```java
package com.codram.terecojo.utils;
 
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
 
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
 
import com.codram.terecojo.DriverActivity;
import com.codram.terecojo.DriverTripsActivity;
import com.codram.terecojo.MyRequestsActivity;
import com.codram.terecojo.NotificationsActivity;
import com.codram.terecojo.R;
import com.codram.terecojo.data.model.ApiResponse;
import com.codram.terecojo.data.model.AuthResponse;
import com.codram.terecojo.data.model.FcmTokenRequest;
import com.codram.terecojo.data.remote.ApiService;
import com.codram.terecojo.data.remote.RetrofitClient;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
 
import java.util.Map;
 
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
 
public class MyFirebaseMessagingService extends FirebaseMessagingService {
 
    private static final String TAG = "MyFirebaseMsgService";
 
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
 
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        updateTokenOnServer(token);
    }
 
    private void updateTokenOnServer(String token) {
        SessionManager sessionManager = SessionManager.getInstance(this);
        if (sessionManager.getToken() != null) {
            ApiService apiService = RetrofitClient.getService();
            apiService.updateFcmToken(new FcmTokenRequest(token)).enqueue(new Callback<ApiResponse<Void>>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "FCM Token actualizado en el servidor");
                    } else {
                        Log.e(TAG, "Error al actualizar FCM Token en el servidor");
                    }
                }
 
                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                    Log.e(TAG, "Fallo de red al actualizar FCM Token: " + t.getMessage());
                }
            });
        }
    }
 
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
}
```
 
---
 
## CORRECCIÓN ADICIONAL EN `NotificationsActivity.java`
 
Mientras implementas lo anterior, hay un bug en la navegación que hace `NotificationsActivity` cuando el usuario toca una notificación in-app (las que están guardadas en BD). El case `nueva_solicitud` lleva al chofer a `DriverOffersActivity` (pantalla vacía) en lugar de `DriverActivity`.
 
**Archivo:** `app/src/main/java/com/codram/terecojo/NotificationsActivity.java`  
**Método:** `navigateBasedOnNotification()`
 
```java
// ANTES (incorrecto)
case "nueva_solicitud":
    intent = new Intent(this, DriverOffersActivity.class); // ← pantalla vacía
    break;
 
// DESPUÉS (correcto)
case "nueva_solicitud":
    intent = new Intent(this, DriverActivity.class); // ← radar del chofer
    break;
```
 
---