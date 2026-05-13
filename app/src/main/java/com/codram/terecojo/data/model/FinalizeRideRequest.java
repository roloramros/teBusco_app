package com.codram.terecojo.data.model;

public class FinalizeRideRequest {
    private int estrellas;
    private String comentario;

    public FinalizeRideRequest(int estrellas, String comentario) {
        this.estrellas = estrellas;
        this.comentario = comentario;
    }

    public int getEstrellas() {
        return estrellas;
    }

    public void setEstrellas(int estrellas) {
        this.estrellas = estrellas;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }
}
