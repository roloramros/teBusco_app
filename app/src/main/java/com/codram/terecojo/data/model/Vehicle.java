package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class Vehicle {
    private String id;
    @SerializedName("chofer_id")
    private String choferId;
    private String marca;
    private String placa;
    private String tipo;
    @SerializedName("capacidad_pasajeros")
    private Integer capacidadPasajeros;
    @SerializedName("foto_url")
    private String fotoUrl;
    private boolean activo;

    // Getters
    public String getId() { return id; }
    public String getMarca() { return marca; }
    public String getPlaca() { return placa; }
    public String getTipo() { return tipo; }
    public Integer getCapacidadPasajeros() { return capacidadPasajeros; }
    public String getFotoUrl() { return fotoUrl; }
}
