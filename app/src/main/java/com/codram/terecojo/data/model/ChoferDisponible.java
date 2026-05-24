package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class ChoferDisponible {

    @SerializedName("chofer_id")
    private String choferId;

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("telefono")
    private String telefono;

    @SerializedName("calificacion_promedio")
    private double calificacionPromedio;

    @SerializedName("total_viajes")
    private int totalViajes;

    @SerializedName("opera_interprovincial")
    private boolean operaInterprovincial;

    @SerializedName("lat")
    private double lat;

    @SerializedName("lng")
    private double lng;

    @SerializedName("ultima_ubicacion_en")
    private String ultima_ubicacion_en;

    @SerializedName("vehiculo_id")
    private String vehiculoId;

    @SerializedName("vehiculo_tipo")
    private String vehiculoTipo;

    @SerializedName("vehiculo_marca")
    private String vehiculoMarca;

    @SerializedName("vehiculo_modelo")
    private String vehiculoModelo;

    @SerializedName("vehiculo_color")
    private String vehiculoColor;

    @SerializedName("vehiculo_placa")
    private String vehiculoPlaca;

    @SerializedName("capacidad_pasajeros")
    private Integer capacidadPasajeros;

    @SerializedName("vehiculo_foto")
    private String vehiculoFoto;

    @SerializedName("distancia_km")
    private Double distanciaKm;

    // Getters
    public String getChoferId()             { return choferId; }
    public String getNombre()               { return nombre; }
    public String getTelefono()             { return telefono; }
    public double getCalificacionPromedio() { return calificacionPromedio; }
    public int getTotalViajes()             { return totalViajes; }
    public boolean isOperaInterprovincial() { return operaInterprovincial; }
    public double getLat()                  { return lat; }
    public double getLng()                  { return lng; }
    public String getUltimaUbicacionEn()    { return ultima_ubicacion_en; }
    public String getVehiculoTipo()         { return vehiculoTipo; }
    public String getVehiculoMarca()        { return vehiculoMarca; }
    public String getVehiculoModelo()       { return vehiculoModelo; }
    public String getVehiculoColor()        { return vehiculoColor; }
    public String getVehiculoPlaca()        { return vehiculoPlaca; }
    public Integer getCapacidadPasajeros()  { return capacidadPasajeros; }
    public String getVehiculoFoto()         { return vehiculoFoto; }
    public Double getDistanciaKm()          { return distanciaKm; }
}
