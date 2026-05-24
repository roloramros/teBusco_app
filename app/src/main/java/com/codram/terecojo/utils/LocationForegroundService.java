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
