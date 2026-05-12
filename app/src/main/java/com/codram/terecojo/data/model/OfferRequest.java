package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class OfferRequest {
    @SerializedName("vehiculo_id")
    private String vehiculoId;
    
    @SerializedName("mensaje")
    private String mensaje;
    
    @SerializedName("precio_propuesto")
    private double precioPropuesto;
    
    @SerializedName("moneda")
    private String moneda;
    
    @SerializedName("tiempo_arribo_min")
    private int tiempoArriboMin;

    public OfferRequest(String vehiculoId, String mensaje, double precioPropuesto, String moneda, int tiempoArriboMin) {
        this.vehiculoId = vehiculoId;
        this.mensaje = mensaje;
        this.precioPropuesto = precioPropuesto;
        this.moneda = moneda;
        this.tiempoArriboMin = tiempoArriboMin;
    }

    // Getters and Setters
    public String getVehiculoId() { return vehiculoId; }
    public void setVehiculoId(String vehiculoId) { this.vehiculoId = vehiculoId; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public double getPrecioPropuesto() { return precioPropuesto; }
    public void setPrecioPropuesto(double precioPropuesto) { this.precioPropuesto = precioPropuesto; }

    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }

    public int getTiempoArriboMin() { return tiempoArriboMin; }
    public void setTiempoArriboMin(int tiempoArriboMin) { this.tiempoArriboMin = tiempoArriboMin; }
}
