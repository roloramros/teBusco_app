package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class VehicleRequest {
    @SerializedName("marca")
    private String marca;
    @SerializedName("placa")
    private String placa;
    @SerializedName("tipo")
    private String tipo;
    @SerializedName("capacidad_pasajeros")
    private Integer capacidadPasajeros;

    public VehicleRequest(String marca, String placa, String tipo, Integer capacidadPasajeros) {
        this.marca = marca;
        this.placa = placa;
        this.tipo = tipo;
        this.capacidadPasajeros = capacidadPasajeros;
    }
}
