package com.codram.terecojo.data.model;
import com.google.gson.annotations.SerializedName;

public class StatsResponse {
    @SerializedName("total_usuarios") public int totalUsuarios;
    @SerializedName("total_choferes") public int totalChoferes;
    @SerializedName("viajes_completados") public int viajesCompletados;
    @SerializedName("viajes_activos") public int viajesActivos;
}
