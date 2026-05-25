package com.codram.terecojo.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.codram.terecojo.DriverActivity;
import com.codram.terecojo.R;
import com.codram.terecojo.data.model.ApiResponse;
import com.codram.terecojo.data.model.UbicacionRequest;
import com.codram.terecojo.data.remote.RetrofitClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
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
    private static final long INTERVAL_MS = 60_000L;
    private static final long MIN_INTERVAL_MS = 30_000L;

    public static final String ACTION_STOP = "ACTION_STOP_LOCATION_SERVICE";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();
        setupLocationCallback();
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null) return;
                Location location = result.getLastLocation();
                if (location != null) {
                    enviarUbicacion(location.getLatitude(), location.getLongitude());
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            detenerService();
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, buildNotification());
        iniciarLocationUpdates();
        return START_STICKY;
    }

    private void iniciarLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, INTERVAL_MS)
                .setMinUpdateIntervalMillis(MIN_INTERVAL_MS)
                .setWaitForAccurateLocation(false)
                .build();

        try {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            );
            Log.d(TAG, "Location updates iniciados cada " + INTERVAL_MS / 1000 + "s");
        } catch (SecurityException e) {
            Log.e(TAG, "Permiso de ubicación revocado: " + e.getMessage());
            detenerService();
        }
    }

    private void enviarUbicacion(double lat, double lng) {
        if (SessionManager.getInstance(this).getToken() == null) {
            Log.d(TAG, "Sin sesión activa, deteniendo service.");
            detenerService();
            return;
        }

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
                            if (data != null && Boolean.TRUE.equals(data.get("debe_detenerse"))) {
                                Log.d(TAG, "Servidor indicó detener el service.");
                                detenerService();
                                LocalBroadcastManager.getInstance(LocationForegroundService.this)
                                        .sendBroadcast(new Intent("com.codram.terecojo.VISIBILIDAD_DESACTIVADA"));
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                        Log.e(TAG, "Error de red al enviar ubicación: " + t.getMessage());
                        // No detener — reintentará en el próximo update del GPS
                    }
                });
    }

    private void detenerService() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        Log.d(TAG, "LocationForegroundService destruido.");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification buildNotification() {
        Intent openIntent = new Intent(this, DriverActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPending = PendingIntent.getActivity(
                this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

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
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Servicio de ubicación activa",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Mantiene tu posición actualizada mientras estás visible en el mapa.");
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);
    }
}