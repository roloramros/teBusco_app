package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

// NUEVO
public class RevocarTodasResponse {
    @SerializedName("sesiones_cerradas") private int sesionesCerradas;
    public int getSesionesCerradas() { return sesionesCerradas; }
}
