package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

// NUEVO
public class SesionActiva {
    @SerializedName("id")          private String id;
    @SerializedName("creado_en")   private String creadoEn;
    @SerializedName("expira_en")   private String expiraEn;
    @SerializedName("es_sesion_actual") private boolean esSesionActual;

    public String getId() { return id; }
    public String getCreadoEn() { return creadoEn; }
    public String getExpiraEn() { return expiraEn; }
    public boolean isEsSesionActual() { return esSesionActual; }
}
