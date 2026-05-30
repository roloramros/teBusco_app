package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class FcmTokenRequest {
    @SerializedName("fcm_token")
    private String fcm_token;

    public FcmTokenRequest(String fcmToken) { this.fcm_token = fcm_token; }
    public String getFcmToken() { return fcm_token; }
    public void setFcmToken(String fcmToken) { this.fcm_token = fcmToken; }
}
