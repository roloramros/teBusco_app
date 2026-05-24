package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class UbicacionRequest {

    @SerializedName("lat")
    private double lat;

    @SerializedName("lng")
    private double lng;

    public UbicacionRequest(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }
}
