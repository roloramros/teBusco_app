package com.codram.terecojo.data.model;

public class LoginRequest {
    private String identificador;
    private String password;
    private String fcm_token;

    public LoginRequest(String identificador, String password, String fcmToken) {
        this.identificador = identificador;
        this.password = password;
        this.fcm_token = fcmToken;
    }
}
