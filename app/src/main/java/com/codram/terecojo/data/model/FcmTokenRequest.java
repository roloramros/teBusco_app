package com.codram.terecojo.data.model;

public class FcmTokenRequest {
    private String fcm_token;

    public FcmTokenRequest(String fcmToken) {
        this.fcm_token = fcmToken;
    }

    public String getFcmToken() {
        return fcm_token;
    }

    public void setFcmToken(String fcmToken) {
        this.fcm_token = fcmToken;
    }
}
