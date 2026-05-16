package com.codram.terecojo;

import android.app.Application;
import com.codram.terecojo.data.remote.RetrofitClient;

// NUEVO
public class TeBuscoApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        RetrofitClient.init(this);
    }
}
