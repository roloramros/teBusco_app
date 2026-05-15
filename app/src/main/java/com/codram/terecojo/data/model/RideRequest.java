package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class RideRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    
    @SerializedName("pasajero_id")
    private String pasajeroId;
    
    @SerializedName("pasajero_nombre")
    private String pasajeroNombre;

    @SerializedName("pasajero_telefono")
    private String pasajeroTelefono;
    
    @SerializedName("origen_descripcion")
    private String origenDescripcion;
    
    @SerializedName("origen_lat")
    private double origenLat;
    
    @SerializedName("origen_lng")
    private double origenLng;
    
    @SerializedName("destino_descripcion")
    private String destinoDescripcion;
    
    @SerializedName("destino_lat")
    private double destinoLat;
    
    @SerializedName("destino_lng")
    private double destinoLng;
    
    // Paradas intermedias (Opción B: JSONB)
    private List<Stop> paradas;
    
    @SerializedName("precio_oferta")
    private double precioOferta;
    
    private List<String> moneda;
    
    @SerializedName("num_pasajeros")
    private int numPasajeros;
    
    @SerializedName("tipo_carga")
    private String tipoCarga;
    
    private String descripcion;
    
    private String estado;
    
    @SerializedName("es_inmediato")
    private boolean esInmediato;
    
    @SerializedName("fecha_viaje")
    private String fechaViaje;
    
    @SerializedName("creada_en")
    private String creadaEn;

    @SerializedName("distancia")
    private double distancia;

    @SerializedName("origen_provincia_nombre")
    private String origenProvinciaNombre;

    @SerializedName("origen_municipio_nombre")
    private String origenMunicipioNombre;

    @SerializedName("destino_provincia_nombre")
    private String destinoProvinciaNombre;

    @SerializedName("destino_municipio_nombre")
    private String destinoMunicipioNombre;

    @SerializedName("origen_municipio_id")
    private Integer origenMunicipioId;

    @SerializedName("num_ofertas")
    private int numOfertas;

    @SerializedName("ha_respondido")
    private boolean haRespondido;

    // Clase interna para las paradas
    public static class Stop implements Serializable {
        private static final long serialVersionUID = 1L;
        private String nombre;
        private double lat;
        private double lng;

        public Stop(String nombre, double lat, double lng) {
            this.nombre = nombre;
            this.lat = lat;
            this.lng = lng;
        }

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }
        public double getLng() { return lng; }
        public void setLng(double lng) { this.lng = lng; }
    }

    // Constructor para nueva solicitud
    public RideRequest() {}

    // Constructor para compatibilidad con datos de prueba
    public RideRequest(String id, String pasajeroNombre, String origenDescripcion, String destinoDescripcion) {
        this.id = id;
        this.pasajeroNombre = pasajeroNombre;
        this.origenDescripcion = origenDescripcion;
        this.destinoDescripcion = destinoDescripcion;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getPasajeroId() { return pasajeroId; }
    public void setPasajeroId(String pasajeroId) { this.pasajeroId = pasajeroId; }
    
    public String getPasajeroNombre() { return pasajeroNombre; }
    public void setPasajeroNombre(String pasajeroNombre) { this.pasajeroNombre = pasajeroNombre; }
    
    public String getPasajeroTelefono() { return pasajeroTelefono; }
    public void setPasajeroTelefono(String pasajeroTelefono) { this.pasajeroTelefono = pasajeroTelefono; }
    
    // Alias para compatibilidad con adaptadores existentes
    public String getPassengerName() { return pasajeroNombre; }
    public String getOrigin() { return origenDescripcion; }
    public String getDestination() { return destinoDescripcion; }
    
    public String getOrigenDescripcion() { return origenDescripcion; }
    public void setOrigenDescripcion(String origenDescripcion) { this.origenDescripcion = origenDescripcion; }
    
    public double getOrigenLat() { return origenLat; }
    public void setOrigenLat(double origenLat) { this.origenLat = origenLat; }
    
    public double getOrigenLng() { return origenLng; }
    public void setOrigenLng(double origenLng) { this.origenLng = origenLng; }
    
    public String getDestinoDescripcion() { return destinoDescripcion; }
    public void setDestinoDescripcion(String destinoDescripcion) { this.destinoDescripcion = destinoDescripcion; }
    
    public double getDestinoLat() { return destinoLat; }
    public void setDestinoLat(double destinoLat) { this.destinoLat = destinoLat; }
    
    public double getDestinoLng() { return destinoLng; }
    public void setDestinoLng(double destinoLng) { this.destinoLng = destinoLng; }
    
    public List<Stop> getParadas() { return paradas; }
    public void setParadas(List<Stop> paradas) { this.paradas = paradas; }
    
    public double getPrecioOferta() { return precioOferta; }
    public void setPrecioOferta(double precioOferta) { this.precioOferta = precioOferta; }
    
    public List<String> getMoneda() { return moneda; }
    public void setMoneda(List<String> moneda) { this.moneda = moneda; }
    
    public int getNumPasajeros() { return numPasajeros; }
    public void setNumPasajeros(int numPasajeros) { this.numPasajeros = numPasajeros; }
    
    public String getTipoCarga() { return tipoCarga; }
    public void setTipoCarga(String tipoCarga) { this.tipoCarga = tipoCarga; }
    
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    
    public boolean isEsInmediato() { return esInmediato; }
    public void setEsInmediato(boolean esInmediato) { this.esInmediato = esInmediato; }
    
    public String getFechaViaje() { return fechaViaje; }
    public void setFechaViaje(String fechaViaje) { this.fechaViaje = fechaViaje; }
    
    public String getCreadaEn() { return creadaEn; }
    public void setCreadaEn(String creadaEn) { this.creadaEn = creadaEn; }

    public double getDistancia() { return distancia; }
    public void setDistancia(double distancia) { this.distancia = distancia; }

    public String getOrigenProvinciaNombre() { return origenProvinciaNombre; }
    public void setOrigenProvinciaNombre(String origenProvinciaNombre) { this.origenProvinciaNombre = origenProvinciaNombre; }

    public String getOrigenMunicipioNombre() { return origenMunicipioNombre; }
    public void setOrigenMunicipioNombre(String origenMunicipioNombre) { this.origenMunicipioNombre = origenMunicipioNombre; }

    public String getDestinoProvinciaNombre() { return destinoProvinciaNombre; }
    public void setDestinoProvinciaNombre(String destinoProvinciaNombre) { this.destinoProvinciaNombre = destinoProvinciaNombre; }

    public String getDestinoMunicipioNombre() { return destinoMunicipioNombre; }
    public void setDestinoMunicipioNombre(String destinoMunicipioNombre) { this.destinoMunicipioNombre = destinoMunicipioNombre; }

    public Integer getOrigenMunicipioId() { return origenMunicipioId; }
    public void setOrigenMunicipioId(Integer origenMunicipioId) { this.origenMunicipioId = origenMunicipioId; }

    public int getNumOfertas() { return numOfertas; }
    public void setNumOfertas(int numOfertas) { this.numOfertas = numOfertas; }

    public boolean isHaRespondido() { return haRespondido; }
    public void setHaRespondido(boolean haRespondido) { this.haRespondido = haRespondido; }
}
