package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    @SerializedName("token")
    private String token;
    
    @SerializedName("usuario")
    private User user;

    public String getToken() { return token; }
    public User getUser() { return user; }

    public static class User {
        private String id;
        private String nombre;
        private String username;
        private String tipo;
        
        @SerializedName("municipio_id")
        private Integer municipioId;
        
        @SerializedName("provincia_id")
        private Integer provinciaId;

        private boolean verificado;

        public String getId() { return id; }
        public String getNombre() { return nombre; }    
        public String getUsername() { return username; }
        public String getTipo() { return tipo; }
        public Integer getMunicipio_id() { return municipioId; }
        public Integer getProvincia_id() { return provinciaId; }
        public boolean isVerificado() { return verificado; }
    }
}
