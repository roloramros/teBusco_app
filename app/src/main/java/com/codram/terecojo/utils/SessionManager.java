package com.codram.terecojo.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import com.codram.terecojo.data.model.ApiResponse;
import com.codram.terecojo.data.model.AuthResponse;
import com.codram.terecojo.data.remote.RetrofitClient;
import com.google.gson.Gson;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SessionManager {
    private static final String PREF_NAME = "TeRecojoPrefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER = "user_data";
    private static final String KEY_TOKEN_SAVED_AT = "token_saved_at"; // NUEVO
    
    private static SessionManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;

    private SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context.getApplicationContext());
        }
        return instance;
    }

    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void saveUser(AuthResponse.User user) {
        String json = gson.toJson(user);
        prefs.edit().putString(KEY_USER, json).apply();
    }

    public void saveSession(String token, AuthResponse.User user) { // NUEVO
        String userJson = gson.toJson(user);
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USER, userJson)
                .putLong(KEY_TOKEN_SAVED_AT, System.currentTimeMillis())
                .apply();
    }

    public AuthResponse.User getUser() {
        String json = prefs.getString(KEY_USER, null);
        if (json == null) return null;
        return gson.fromJson(json, AuthResponse.User.class);
    }

    public long getTokenSavedAt() { // NUEVO
        return prefs.getLong(KEY_TOKEN_SAVED_AT, 0L);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    public void logout(Context context) {
        // Desuscribir de notificaciones si es chofer
        AuthResponse.User user = getUser();
        if (user != null && "chofer".equalsIgnoreCase(user.getTipo()) && user.getMunicipio_id() != null) {
            String topic = "municipio_" + user.getMunicipio_id();
            com.google.firebase.messaging.FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("FCM", "Desuscrito exitosamente de: " + topic);
                        }
                    });
        }

        // Intentar avisar al servidor para invalidar el token
        RetrofitClient.getService(context).logout().enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                Log.d("TeRecojo", "Sesión cerrada en el servidor");
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e("TeRecojo", "No se pudo cerrar sesión en el servidor: " + t.getMessage());
            }
        });

        // Limpiar localmente siempre, independientemente de si la API falló
        clear();
        Intent intent = new Intent(context, com.codram.terecojo.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("skip_auto_login", true);
        context.startActivity(intent);
    }
}
