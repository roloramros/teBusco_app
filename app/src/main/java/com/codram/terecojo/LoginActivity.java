package com.codram.terecojo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.codram.terecojo.data.model.ApiResponse;
import com.codram.terecojo.data.model.AuthResponse;
import com.codram.terecojo.data.model.FcmTokenRequest;
import com.codram.terecojo.data.model.LoginRequest;
import com.codram.terecojo.data.model.StatsResponse;
import com.codram.terecojo.data.remote.RetrofitClient;
import com.codram.terecojo.databinding.ActivityLoginBinding;
import com.codram.terecojo.utils.ParallaxStatsManager;
import com.codram.terecojo.utils.SessionManager;
import com.google.firebase.messaging.FirebaseMessaging;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.codram.terecojo.utils.ErrorUtils;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private ParallaxStatsManager parallaxManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Parallax Stats Background
        parallaxManager = new ParallaxStatsManager(binding.parallaxContainer);
        parallaxManager.init(15);
        fetchStats();

        boolean skipAutoLogin = getIntent().getBooleanExtra("skip_auto_login", false);
        if (!skipAutoLogin) {
            checkAutoLogin();
        }

        binding.tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        binding.btnEnter.setOnClickListener(v -> onLoginClicked());
    }

    private void fetchStats() {
        RetrofitClient.getService(this).getStats().enqueue(new Callback<ApiResponse<StatsResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<StatsResponse>> call, Response<ApiResponse<StatsResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isOk()) {
                    parallaxManager.updateData(response.body().getData());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<StatsResponse>> call, Throwable t) {
                Log.e("TeRecojo", "Error fetching stats: " + t.getMessage());
            }
        });
    }

    private void checkAutoLogin() {
        SessionManager sessionManager = SessionManager.getInstance(this);
        if (sessionManager.getToken() != null) {
            Log.d("TeRecojo", "Token encontrado, intentando auto-login...");
            RetrofitClient.getService(this).getMe().enqueue(new Callback<ApiResponse<AuthResponse.User>>() {
                @Override
                public void onResponse(Call<ApiResponse<AuthResponse.User>> call, Response<ApiResponse<AuthResponse.User>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isOk()) {
                        Log.d("TeRecojo", "Auto-login exitoso");
                        AuthResponse.User user = response.body().getData();
                        
                        // Actualizar Token de FCM en auto-login
                        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                String token = task.getResult();
                                RetrofitClient.getService(LoginActivity.this)
                                        .updateFcmToken(new FcmTokenRequest(token))
                                        .enqueue(new Callback<ApiResponse<Void>>() {
                                            @Override
                                            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                                                Log.d("FCM", "Token actualizado en auto-login");
                                            }

                                            @Override
                                            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                                                Log.e("FCM", "Error actualizando token en auto-login");
                                            }
                                        });
                            }
                        });

                        navigateToMain(user);
                    } else {
                        Log.d("TeRecojo", "Auto-login fallido o token expirado");
                        sessionManager.clear();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<AuthResponse.User>> call, Throwable t) {
                    Log.e("TeRecojo", "Error de red en auto-login: " + t.getMessage());
                }
            });
        }
    }

    private void onLoginClicked() {
        String user = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (user.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnEnter.setEnabled(false);
        
        // Obtener el Token de Firebase antes de loguear
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            String token = task.isSuccessful() ? task.getResult() : null;
            executeLogin(user, password, token);
        });
    }

    private void executeLogin(String user, String password, String fcmToken) {
        Log.d("TeRecojo", "Intentando login para: " + user + " con FCM Token: " + (fcmToken != null ? "SI" : "NO"));
        LoginRequest request = new LoginRequest(user, password, fcmToken);

        RetrofitClient.getService(this).login(request).enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call, Response<ApiResponse<AuthResponse>> response) {
                binding.btnEnter.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isOk()) {
                    AuthResponse auth = response.body().getData();
                    
                    // Si es chofer, suscribirlo a su municipio
                    if (auth.getUser().getTipo().equalsIgnoreCase("chofer") && auth.getUser().getMunicipio_id() != null) {
                        String topic = "municipio_" + auth.getUser().getMunicipio_id();
                        FirebaseMessaging.getInstance().subscribeToTopic(topic);
                        Log.d("FCM", "Suscrito al tema: " + topic);
                    }

                    SessionManager sessionManager = SessionManager.getInstance(LoginActivity.this);
                    if (binding.cbRememberMe.isChecked()) {
                        sessionManager.saveToken(auth.getToken());
                    }
                    sessionManager.saveUser(auth.getUser());

                    Toast.makeText(LoginActivity.this, "Bienvenido " + auth.getUser().getNombre(), Toast.LENGTH_LONG).show();
                    navigateToMain(auth.getUser());
                } else {
                    String msg = ErrorUtils.parseError(response, "Error al iniciar sesión");
                    Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                binding.btnEnter.setEnabled(true);
                Log.e("TeRecojo", "Fallo en login: " + t.getMessage());
                Toast.makeText(LoginActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToMain(AuthResponse.User user) {
        String role = user.getTipo();
        String nombre = user.getNombre();

        Intent intent;
        if ("chofer".equalsIgnoreCase(role)) {
            intent = new Intent(LoginActivity.this, DriverProfileActivity.class);
        } else if ("admin".equalsIgnoreCase(role)) {
            intent = new Intent(LoginActivity.this, AdminActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, MainActivity.class);
        }

        intent.putExtra("nombre", nombre);
        startActivity(intent);
        finish();
    }
}
