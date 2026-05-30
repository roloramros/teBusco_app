package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class RejectOfferRequest {
    @SerializedName("motivo")
    private String motivo;

    public RejectOfferRequest(String motivo) { this.motivo = motivo; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
