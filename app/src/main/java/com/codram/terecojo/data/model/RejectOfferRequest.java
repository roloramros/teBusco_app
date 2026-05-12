package com.codram.terecojo.data.model;

public class RejectOfferRequest {
    private String motivo;

    public RejectOfferRequest(String motivo) {
        this.motivo = motivo;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
