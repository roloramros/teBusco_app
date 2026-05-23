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
import com.codram.terecojo.MainActivity;
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
            ApiService apiService = RetrofitClient.getService(); // MODIFICADO;
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
}
