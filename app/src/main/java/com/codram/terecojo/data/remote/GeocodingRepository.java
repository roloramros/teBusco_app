package com.codram.terecojo.data.remote;

import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeocodingRepository {
    private static final String API_KEY = "AIzaSyBufiSwuBW19JLsbXKDbW86pg_1wL7ifxU";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface GeocodeCallback {
        void onSuccess(String address);
        void onError(String error);
    }

    public void reverseGeocode(LatLng point, GeocodeCallback callback) {
        executor.execute(() -> {
            try {
                String urlStr = "https://maps.googleapis.com/maps/api/geocode/json?latlng="
                        + point.latitude + "," + point.longitude
                        + "&key=" + API_KEY;

                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                InputStream is = conn.getInputStream();
                String json = new String(is.readAllBytes());
                is.close();

                JSONObject obj = new JSONObject(json);
                String status = obj.getString("status");

                if (status.equals("OK")) {
                    JSONArray results = obj.getJSONArray("results");
                    String addressLine = null;
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject result = results.getJSONObject(i);
                        JSONArray types = result.getJSONArray("types");
                        String firstType = types.getString(0);
                        if (!firstType.equals("plus_code")) {
                            addressLine = result.getString("formatted_address");
                            break;
                        }
                    }
                    if (addressLine == null) {
                        addressLine = results.getJSONObject(0).getString("formatted_address");
                    }
                    callback.onSuccess(addressLine);
                } else {
                    callback.onSuccess(String.format(Locale.getDefault(), "%.5f, %.5f", point.latitude, point.longitude));
                }
            } catch (Exception e) {
                Log.e("GeocodingRepository", "Error", e);
                callback.onError(e.getMessage());
            }
        });
    }
}
