package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class VehicleRequest {
    private String marca;
    private String placa;
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
