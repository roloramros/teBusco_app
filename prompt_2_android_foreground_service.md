# PROMPT 2 — Android: ForegroundService de Ubicación + Toggle de Visibilidad
**Proyecto:** Te Busco App (Android Java)  
**Objetivo:** Implementar el servicio de background que mantiene la posición del chofer actualizada mientras está visible, y el toggle en `DriverActivity` que lo activa/desactiva con selección de vehículo.

---

## CONTEXTO

Cuando el chofer activa "Visible en el mapa":
1. Si tiene más de un vehículo → se muestra un dialog para elegir cuál
2. Se inicia un `ForegroundService` con notificación persistente
3. El service envía la posición GPS al servidor cada 60 segundos
4. Si el servidor responde `debe_detenerse: true` → el service se detiene solo
5. Al desactivar → el service se detiene y se notifica al servidor

---

## PARTE 1 — PERMISOS EN `AndroidManifest.xml`

Agregar estos dos permisos junto a los existentes:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
```

Registrar el nuevo service dentro de `<application>`, junto a `MyFirebaseMessagingService`:

```xml
<service
    android:name=".utils.LocationForegroundService"
    android:foregroundServiceType="location"
    android:exported="false" />
```

---

## PARTE 2 — NUEVO ARCHIVO: `app/src/main/java/com/codram/terecojo/data/model/ToggleVisibilidadRequest.java`

```java
package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class ToggleVisibilidadRequest {

    @SerializedName("visible")
    private boolean visible;

    @SerializedName("lat")
    private Double lat;

    @SerializedName("lng")
    private Double lng;

    @SerializedName("vehiculo_id")
    private String vehiculoId;

    public ToggleVisibilidadRequest(boolean visible) {
        this.visible = visible;
    }

    public ToggleVisibilidadRequest(boolean visible, double lat, double lng, String vehiculoId) {
        this.visible = visible;
        this.lat = lat;
        this.lng = lng;
        this.vehiculoId = vehiculoId;
    }
}
```

---

## PARTE 3 — NUEVO ARCHIVO: `app/src/main/java/com/codram/terecojo/data/model/UbicacionRequest.java`

```java
package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class UbicacionRequest {

    @SerializedName("lat")
    private double lat;

    @SerializedName("lng")
    private double lng;

    public UbicacionRequest(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }
}
```

---

## PARTE 4 — MODIFICAR: `app/src/main/java/com/codram/terecojo/data/remote/ApiService.java`

Agregar las dos nuevas llamadas junto a las demás:

```java
// Agregar estos dos imports si no están:
import com.codram.terecojo.data.model.ToggleVisibilidadRequest;
import com.codram.terecojo.data.model.UbicacionRequest;

// Agregar estas declaraciones:
@PUT("api/choferes/visibilidad")
Call<ApiResponse<Void>> toggleVisibilidad(@Body ToggleVisibilidadRequest request);

@PUT("api/choferes/ubicacion")
Call<ApiResponse<java.util.Map<String, Object>>> actualizarUbicacion(@Body UbicacionRequest request);
```

---

## PARTE 5 — NUEVO ARCHIVO: `app/src/main/java/com/codram/terecojo/utils/LocationForegroundService.java`

Este es el servicio central. Crear el archivo completo:

```java
package com.codram.terecojo.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.codram.terecojo.DriverActivity;
import com.codram.terecojo.R;
import com.codram.terecojo.data.model.ApiResponse;
import com.codram.terecojo.data.model.UbicacionRequest;
import com.codram.terecojo.data.remote.RetrofitClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocationForegroundService extends Service {

    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "location_service_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final long INTERVAL_MS = 60_000; // 60 segundos

    // Action para detener el service desde la notificación
    public static final String ACTION_STOP = "ACTION_STOP_LOCATION_SERVICE";

    private Handler handler;
    private Runnable locationRunnable;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // El chofer tocó "Detener" en la notificación
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            detenerService();
            return START_NOT_STICKY;
        }

        // Iniciar en foreground con la notificación persistente
        startForeground(NOTIFICATION_ID, buildNotification());

        // Iniciar el loop de actualización de ubicación
        iniciarLoop();

        // START_STICKY → Android reinicia el service si lo mata (batería, etc.)
        return START_STICKY;
    }

    private void iniciarLoop() {
        locationRunnable = new Runnable() {
            @Override
            public void run() {
                enviarUbicacion();
                handler.postDelayed(this, INTERVAL_MS);
            }
        };
        // Primera ejecución inmediata
        handler.post(locationRunnable);
    }

    private void enviarUbicacion() {
        if (SessionManager.getInstance(this).getToken() == null) {
            Log.d(TAG, "Sin sesión activa, deteniendo service.");
            detenerService();
            return;
        }

        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        Log.w(TAG, "No se obtuvo ubicación.");
                        return;
                    }

                    double lat = location.getLatitude();
                    double lng = location.getLongitude();
                    Log.d(TAG, "Enviando ubicación: " + lat + ", " + lng);

                    RetrofitClient.getService()
                        .actualizarUbicacion(new UbicacionRequest(lat, lng))
                        .enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
                            @Override
                            public void onResponse(
                                Call<ApiResponse<Map<String, Object>>> call,
                                Response<ApiResponse<Map<String, Object>>> response
                            ) {
                                if (response.isSuccessful() && response.body() != null) {
                                    Map<String, Object> data = response.body().getData();
                                    // Si el servidor indica que debemos detenernos
                                    // (licencia expirada, admin bloqueó, etc.)
                                    if (data != null) {
                                        Object debeDetenerse = data.get("debe_detenerse");
                                        if (Boolean.TRUE.equals(debeDetenerse)) {
                                            Log.d(TAG, "Servidor indicó detener el service.");
                                            detenerService();
                                            // Notificar a la Activity para que actualice el toggle
                                            Intent broadcast = new Intent("com.codram.terecojo.VISIBILIDAD_DESACTIVADA");
                                            sendBroadcast(broadcast);
                                        }
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                                Log.e(TAG, "Error de red al enviar ubicación: " + t.getMessage());
                                // No detener el service por error de red — reintentará en 60s
                            }
                        });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al obtener ubicación: " + e.getMessage());
                });
        } catch (SecurityException e) {
            Log.e(TAG, "Permiso de ubicación revocado: " + e.getMessage());
            detenerService();
        }
    }

    private void detenerService() {
        handler.removeCallbacks(locationRunnable);
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && locationRunnable != null) {
            handler.removeCallbacks(locationRunnable);
        }
        Log.d(TAG, "LocationForegroundService destruido.");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // No es un bound service
    }

    // — Notificación persistente —

    private Notification buildNotification() {
        // Al tocar la notificación → abre DriverActivity
        Intent openIntent = new Intent(this, DriverActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Botón "Detener" en la notificación
        Intent stopIntent = new Intent(this, LocationForegroundService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Te Busco — Visible en el mapa")
            .setContentText("Los pasajeros pueden encontrarte. Toca para abrir la app.")
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentIntent(openPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Desactivar", stopPending)
            .setOngoing(true)       // No se puede deslizar para cerrar
            .setPriority(NotificationCompat.PRIORITY_LOW)  // Sin sonido
            .build();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID,
            "Servicio de ubicación activa",
            NotificationManager.IMPORTANCE_LOW  // Sin sonido ni vibración
        );
        channel.setDescription("Mantiene tu posición actualizada mientras estás visible en el mapa.");
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
```

---

## PARTE 6 — MODIFICAR: `app/src/main/java/com/codram/terecojo/DriverActivity.java`

### Cambio 6A — Nuevas variables de instancia

Agregar junto a las variables existentes al inicio de la clase:

```java
// Variables para visibilidad en mapa
private boolean estaVisible = false;
private android.content.BroadcastReceiver visibilidadReceiver;
```

### Cambio 6B — En `onCreate()`, después de `setupRefreshRadarButton()`

Agregar:

```java
setupVisibilidadToggle();
registrarVisibilidadReceiver();
```

### Cambio 6C — Nuevo método `setupVisibilidadToggle()`

```java
private void setupVisibilidadToggle() {
    // El FAB de visibilidad — se agrega al layout en el Paso 6F
    if (binding.fabVisibilidad == null) return;

    binding.fabVisibilidad.setOnClickListener(v -> {
        if (estaVisible) {
            // Desactivar
            desactivarVisibilidad();
        } else {
            // Activar — pedir ubicación y mostrar dialog de vehículo si aplica
            activarVisibilidad();
        }
    });
}
```

### Cambio 6D — Nuevo método `activarVisibilidad()`

```java
private void activarVisibilidad() {
    // Verificar permiso de ubicación
    if (androidx.core.content.ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.ACCESS_FINE_LOCATION)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(this, "Se necesita permiso de ubicación para activarte en el mapa", Toast.LENGTH_LONG).show();
        return;
    }

    // Obtener la última ubicación conocida para enviarla al activarse
    com.google.android.gms.location.FusedLocationProviderClient client =
        com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this);

    try {
        client.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                Toast.makeText(this, "No pudimos obtener tu ubicación. Asegúrate de tener el GPS activo.", Toast.LENGTH_LONG).show();
                return;
            }

            double lat = location.getLatitude();
            double lng = location.getLongitude();

            if (myVehicles.isEmpty()) {
                // Sin vehículos → activar sin vehiculo_id
                confirmarActivacion(lat, lng, null);
            } else if (myVehicles.size() == 1) {
                // Un solo vehículo → activar directamente
                confirmarActivacion(lat, lng, myVehicles.get(0).getId());
            } else {
                // Varios vehículos → mostrar dialog de selección
                mostrarDialogSeleccionVehiculo(lat, lng);
            }
        });
    } catch (SecurityException e) {
        Toast.makeText(this, "Error de permisos de ubicación", Toast.LENGTH_SHORT).show();
    }
}
```

### Cambio 6E — Nuevo método `mostrarDialogSeleccionVehiculo()`

```java
private void mostrarDialogSeleccionVehiculo(double lat, double lng) {
    String[] nombres = myVehicles.stream()
        .map(v -> v.getMarca() + " · " + v.getPlaca()
            + (v.getCapacidadPasajeros() != null ? " · " + v.getCapacidadPasajeros() + " pax" : ""))
        .toArray(String[]::new);

    new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
        .setTitle("¿Con qué vehículo sales hoy?")
        .setItems(nombres, (dialog, which) -> {
            String vehiculoId = myVehicles.get(which).getId();
            confirmarActivacion(lat, lng, vehiculoId);
        })
        .setNegativeButton("Cancelar", null)
        .show();
}
```

### Cambio 6F — Nuevo método `confirmarActivacion()`

```java
private void confirmarActivacion(double lat, double lng, String vehiculoId) {
    ToggleVisibilidadRequest request = new ToggleVisibilidadRequest(true, lat, lng, vehiculoId);

    RetrofitClient.getService().toggleVisibilidad(request).enqueue(
        new retrofit2.Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    estaVisible = true;
                    actualizarBotonVisibilidad();
                    // Iniciar el ForegroundService
                    Intent serviceIntent = new Intent(DriverActivity.this,
                        com.codram.terecojo.utils.LocationForegroundService.class);
                    startForegroundService(serviceIntent);
                    Toast.makeText(DriverActivity.this,
                        "¡Estás visible! Los pasajeros pueden encontrarte.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(DriverActivity.this,
                        "No se pudo activar la visibilidad. Verifica tu licencia.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(DriverActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        }
    );
}
```

### Cambio 6G — Nuevo método `desactivarVisibilidad()`

```java
private void desactivarVisibilidad() {
    ToggleVisibilidadRequest request = new ToggleVisibilidadRequest(false);

    RetrofitClient.getService().toggleVisibilidad(request).enqueue(
        new retrofit2.Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                // Detener el service independientemente de la respuesta
                Intent serviceIntent = new Intent(DriverActivity.this,
                    com.codram.terecojo.utils.LocationForegroundService.class);
                stopService(serviceIntent);

                estaVisible = false;
                actualizarBotonVisibilidad();
                Toast.makeText(DriverActivity.this,
                    "Ya no eres visible en el mapa.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                // Aunque falle la red, detener el service localmente
                Intent serviceIntent = new Intent(DriverActivity.this,
                    com.codram.terecojo.utils.LocationForegroundService.class);
                stopService(serviceIntent);
                estaVisible = false;
                actualizarBotonVisibilidad();
            }
        }
    );
}
```

### Cambio 6H — Nuevo método `actualizarBotonVisibilidad()`

```java
private void actualizarBotonVisibilidad() {
    if (binding.fabVisibilidad == null) return;
    if (estaVisible) {
        binding.fabVisibilidad.setImageResource(android.R.drawable.presence_online);
        binding.fabVisibilidad.setBackgroundTintList(
            androidx.core.content.ContextCompat.getColorStateList(this, R.color.success_green));
        binding.fabVisibilidad.setContentDescription("Desactivar visibilidad en el mapa");
    } else {
        binding.fabVisibilidad.setImageResource(android.R.drawable.presence_invisible);
        binding.fabVisibilidad.setBackgroundTintList(
            androidx.core.content.ContextCompat.getColorStateList(this, R.color.gray_dark));
        binding.fabVisibilidad.setContentDescription("Activar visibilidad en el mapa");
    }
}
```

### Cambio 6I — Nuevo método `registrarVisibilidadReceiver()`

Este receiver escucha el broadcast que envía el ForegroundService cuando el servidor le indica que se detenga (licencia expirada, etc.):

```java
private void registrarVisibilidadReceiver() {
    visibilidadReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            // El service fue detenido remotamente
            estaVisible = false;
            actualizarBotonVisibilidad();
            Toast.makeText(DriverActivity.this,
                "Tu visibilidad fue desactivada automáticamente.", Toast.LENGTH_LONG).show();
        }
    };

    androidx.localbroadcastmanager.content.LocalBroadcastManager
        .getInstance(this)
        .registerReceiver(visibilidadReceiver,
            new android.content.IntentFilter("com.codram.terecojo.VISIBILIDAD_DESACTIVADA"));
}
```

### Cambio 6J — En `onDestroy()` — desregistrar el receiver

Si `DriverActivity` no tiene `onDestroy()`, crearlo. Si ya existe, agregar al final:

```java
@Override
protected void onDestroy() {
    super.onDestroy();
    if (visibilidadReceiver != null) {
        androidx.localbroadcastmanager.content.LocalBroadcastManager
            .getInstance(this)
            .unregisterReceiver(visibilidadReceiver);
    }
}
```

---

## PARTE 7 — MODIFICAR: `app/src/main/res/layout/activity_driver.xml`

Agregar el FAB de visibilidad en el `ConstraintLayout` interior (`@id/main_content`), encima del FAB de refresh. Agregar **después** del FAB `fabRefreshRadar` y **antes** del FAB `fabClearRoute`:

```xml
<com.google.android.material.floatingactionbutton.FloatingActionButton
    android:id="@+id/fabVisibilidad"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_margin="16dp"
    android:contentDescription="Activar visibilidad en el mapa"
    android:src="@android:drawable/presence_invisible"
    app:backgroundTint="@color/gray_dark"
    app:layout_constraintBottom_toTopOf="@id/fabRefreshRadar"
    app:layout_constraintEnd_toEndOf="parent"
    app:tint="@color/white" />
```

---

## PARTE 8 — Agregar dependencia en `app/build.gradle.kts`

El `LocalBroadcastManager` requiere una dependencia explícita. Agregar en el bloque `dependencies`:

```kotlin
implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
```

---

## RESUMEN DE ARCHIVOS

| Archivo | Tipo |
|---|---|
| `AndroidManifest.xml` | ✏️ 2 permisos + 1 `<service>` |
| `data/model/ToggleVisibilidadRequest.java` | ✅ Nuevo |
| `data/model/UbicacionRequest.java` | ✅ Nuevo |
| `data/remote/ApiService.java` | ✏️ 2 declaraciones nuevas |
| `utils/LocationForegroundService.java` | ✅ Nuevo |
| `DriverActivity.java` | ✏️ Variables + 8 métodos nuevos |
| `res/layout/activity_driver.xml` | ✏️ 1 FAB nuevo |
| `app/build.gradle.kts` | ✏️ 1 dependencia |

---

## FLUJO COMPLETO ESPERADO

1. Chofer abre `DriverActivity` → ve el FAB gris (invisible)
2. Toca el FAB → si tiene 2+ vehículos, aparece dialog de selección
3. Elige vehículo → se llama a `PUT /api/choferes/visibilidad`
4. El FAB cambia a verde → se inicia `LocationForegroundService`
5. Aparece notificación persistente: "Te Busco — Visible en el mapa"
6. Cada 60s el service envía GPS al servidor silenciosamente
7. El chofer cierra la app → el service sigue corriendo en background
8. El chofer toca "Desactivar" en la notificación → service se detiene, servidor actualiza
9. Si la licencia expira → servidor responde `debe_detenerse: true` → service se detiene solo → FAB vuelve a gris
