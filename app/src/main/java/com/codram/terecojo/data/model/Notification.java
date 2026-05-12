package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class Notification {
    @SerializedName("id")
    private String id;

    @SerializedName("tipo")
    private String tipo;

    @SerializedName("titulo")
    private String titulo;

    @SerializedName("cuerpo")
    private String cuerpo;

    @SerializedName("leida")
    private boolean leida;

    @SerializedName("creada_en")
    private String creadaEn;

    @SerializedName("datos_extra")
    private Map<String, Object> datosExtra;

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getCuerpo() { return cuerpo; }
    public void setCuerpo(String cuerpo) { this.cuerpo = cuerpo; }

    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }

    public String getCreadaEn() { return creadaEn; }
    public void setCreadaEn(String creadaEn) { this.creadaEn = creadaEn; }

    public Map<String, Object> getDatosExtra() { return datosExtra; }
    public void setDatosExtra(Map<String, Object> datosExtra) { this.datosExtra = datosExtra; }

    public String getSolicitudId() {
        if (datosExtra != null && datosExtra.containsKey("solicitud_id")) {
            return datosExtra.get("solicitud_id").toString();
        }
        return null;
    }
}
