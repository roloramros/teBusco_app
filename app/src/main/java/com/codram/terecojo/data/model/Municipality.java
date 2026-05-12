package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class Municipality {
    @SerializedName("id")
    private int id;
    
    @SerializedName("nombre")
    private String name;
    
    @SerializedName("codigo")
    private String code;

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }

    @Override
    public String toString() {
        return name; // Para que el Spinner muestre el nombre
    }
}
