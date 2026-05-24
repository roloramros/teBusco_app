package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class ToggleVisibilidadRequest {

    @SerializedName("visible")
    private boolean visible;

    @SerializedName("lat")
    private Double lat;

    @SerializedName("lng")
    private Double lng;

    @SerializedName("vehiculo_id")
    private String vehiculoId;

    public ToggleVisibilidadRequest(boolean visible) {
        this.visible = visible;
    }

    public ToggleVisibilidadRequest(boolean visible, double lat, double lng, String vehiculoId) {
        this.visible = visible;
        this.lat = lat;
        this.lng = lng;
        this.vehiculoId = vehiculoId;
    }
}
