package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    @SerializedName("nombre")
    private String nombre;
    @SerializedName("username")
    private String username;
    @SerializedName("telefono")
    private String telefono;
    @SerializedName("email")
    private String email;
    @SerializedName("password")
    private String password;
    @SerializedName("tipo")
    private String tipo;
    @SerializedName("provincia_id")
    private Integer provincia_id;
    @SerializedName("municipio_id")
    private Integer municipio_id;
    @SerializedName("fcm_token")
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
