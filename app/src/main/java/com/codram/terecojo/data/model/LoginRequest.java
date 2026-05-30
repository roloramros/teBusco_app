package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {
    @SerializedName("identificador")
    private String identificador;
    @SerializedName("password")
    private String password;
    @SerializedName("fcm_token")
    private String fcm_token;

    public LoginRequest(String identificador, String password, String fcmToken) {
        this.identificador = identificador;
        this.password = password;
        this.fcm_token = fcmToken;
    }
}
