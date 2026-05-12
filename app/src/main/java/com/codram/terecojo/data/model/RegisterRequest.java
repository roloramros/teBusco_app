package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    private String nombre;
    private String username;
    private String telefono;
    private String email;
    private String password;
    private String tipo;
    private Integer provincia_id;
    private Integer municipio_id;
    private String fcm_token;

    public RegisterRequest(String nombre, String username, String telefono, String email, 
                           String password, String tipo, Integer provincia_id, Integer municipio_id, String fcmToken) {
        this.nombre = nombre;
        this.username = username;
        this.telefono = telefono;
        this.email = email;
        this.password = password;
        this.tipo = tipo;
        this.provincia_id = provincia_id;
        this.municipio_id = municipio_id;
        this.fcm_token = fcmToken;
    }
}
