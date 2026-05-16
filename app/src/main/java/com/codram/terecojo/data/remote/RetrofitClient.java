package com.codram.terecojo.data.remote;

import android.content.Context;

import com.codram.terecojo.BuildConfig;
import com.codram.terecojo.utils.SessionManager;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // Usar 10.0.2.2 para acceder al localhost de la máquina desde el emulador Android
    private static final String BASE_URL = BuildConfig.API_BASE_URL;
    
    // NUEVO
    private static Context appContext = null;

    // MODIFICADO — volatile garantiza visibilidad entre hilos
    private static volatile Retrofit retrofit = null;
    private static volatile ApiService apiService = null;

    // NUEVO
    public static void init(Context context) {
        if (appContext == null) {
            appContext = context.getApplicationContext();
        }
    }

    // MODIFICADO
    public static ApiService getService() {
        if (apiService == null) {
            synchronized (RetrofitClient.class) {
                if (apiService == null) {
                    if (appContext == null) {
                        throw new IllegalStateException(
                            "RetrofitClient no inicializado. Llama a RetrofitClient.init(context) en Application.onCreate()"
                        );
                    }
                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    logging.setLevel(BuildConfig.DEBUG
                        ? HttpLoggingInterceptor.Level.BODY
                        : HttpLoggingInterceptor.Level.NONE);

                    OkHttpClient client = new OkHttpClient.Builder()
                        .addInterceptor(chain -> {
                            SessionManager session = SessionManager.getInstance(appContext);
                            String token = session.getToken();
                            okhttp3.Request.Builder builder = chain.request().newBuilder()
                                .addHeader("Accept", "application/json");
                            
                            if (token != null && !token.trim().isEmpty()) {
                                builder.addHeader("Authorization", "Bearer " + token);
                            }
                            
                            return chain.proceed(builder.build());
                        })
                        .addInterceptor(logging)
                        .build();

                    retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create(
                            new com.google.gson.GsonBuilder()
                                .setLenient()
                                .create()
                        ))
                        .build();

                    apiService = retrofit.create(ApiService.class);
                }
            }
        }
        return apiService;
    }
}
