package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class Offer {
    private String id;
    @SerializedName("solicitud_id")
    private String solicitudId;
    @SerializedName("chofer_id")
    private String choferId;
    @SerializedName("vehiculo_id")
    private String vehiculoId;
    private String mensaje;
    @SerializedName("precio_propuesto")
    private double precioPropuesto;
    private String moneda;
    @SerializedName("tiempo_arribo_min")
    private int tiempoArriboMin;
    private String estado;
    @SerializedName("respondido_en")
    private String respondidoEn;

    // Campos extra del JOIN en el backend
    @SerializedName("chofer_nombre")
    private String choferNombre;
    @SerializedName("chofer_foto")
    private String choferFoto;
    @SerializedName("calificacion_promedio")
    private double calificacionPromedio;
    
    @SerializedName("marca")
    private String vehiculoMarca;
    @SerializedName("placa")
    private String vehiculoPlaca;
    @SerializedName("vehiculo_tipo")
    private String vehiculoTipo;
    @SerializedName("vehiculo_foto")
    private String vehiculoFoto;

    // Getters
    public String getId() { return id; }
    public String getSolicitudId() { return solicitudId; }
    public String getChoferId() { return choferId; }
    public String getVehiculoId() { return vehiculoId; }
    public String getMensaje() { return mensaje; }
    public double getPrecioPropuesto() { return precioPropuesto; }
    public String getMoneda() { return moneda; }
    public int getTiempoArriboMin() { return tiempoArriboMin; }
    public String getEstado() { return estado; }
    public String getRespondidoEn() { return respondidoEn; }
    public String getChoferNombre() { return choferNombre; }
    public String getChoferFoto() { return choferFoto; }
    public double getCalificacionPromedio() { return calificacionPromedio; }
    public String getVehiculoMarca() { return vehiculoMarca; }
    public String getVehiculoPlaca() { return vehiculoPlaca; }
    public String getVehiculoTipo() { return vehiculoTipo; }
    public String getVehiculoFoto() { return vehiculoFoto; }
}
