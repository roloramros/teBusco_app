package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class ApiResponse<T> {
    @SerializedName("ok")
    private boolean ok;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("data")
    private T data;

    public boolean isOk() { return ok; }
    public String getMessage() { return message; }
    public T getData() { return data; }
}
