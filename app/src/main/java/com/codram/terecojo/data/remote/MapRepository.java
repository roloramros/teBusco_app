package com.codram.terecojo.data.remote;

import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapRepository {
    private static final String API_KEY = "AIzaSyBufiSwuBW19JLsbXKDbW86pg_1wL7ifxU";
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public interface MapCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    // ✅ Helper compatible con todos los niveles de API
    private byte[] readAllBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, bytesRead);
        }
        return buffer.toByteArray();
    }

    public void reverseGeocode(LatLng point, MapCallback<com.codram.terecojo.data.model.LocationDetails> callback) {
        executor.execute(() -> {
            try {
                String urlStr = "https://maps.googleapis.com/maps/api/geocode/json?latlng="
                        + point.latitude + "," + point.longitude
                        + "&key=" + API_KEY;

                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                InputStream is = conn.getInputStream();
                String json = new String(readAllBytes(is));
                is.close();

                JSONObject obj = new JSONObject(json);
                if (obj.getString("status").equals("OK")) {
                    JSONArray results = obj.getJSONArray("results");
                    JSONObject bestResult = null;
                    
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject result = results.getJSONObject(i);
                        JSONArray types = result.getJSONArray("types");
                        boolean isPlusCode = false;
                        for(int j=0; j<types.length(); j++) if(types.getString(j).equals("plus_code")) isPlusCode = true;
                        if (!isPlusCode) {
                            bestResult = result;
                            break;
                        }
                    }

                    if (bestResult == null) bestResult = results.getJSONObject(0);

                    String formattedAddress = bestResult.getString("formatted_address");
                    String province = "";
                    String municipality = "";

                    JSONArray addressComponents = bestResult.getJSONArray("address_components");
                    for (int i = 0; i < addressComponents.length(); i++) {
                        JSONObject comp = addressComponents.getJSONObject(i);
                        JSONArray types = comp.getJSONArray("types");
                        for (int j = 0; j < types.length(); j++) {
                            String type = types.getString(j);
                            if (type.equals("administrative_area_level_1")) {
                                province = comp.getString("long_name");
                            } else if (type.equals("administrative_area_level_2") || type.equals("locality")) {
                                // En Cuba, level_2 suele ser el municipio. Locality también puede serlo.
                                municipality = comp.getString("long_name");
                            }
                        }
                    }

                    callback.onSuccess(new com.codram.terecojo.data.model.LocationDetails(formattedAddress, province, municipality));
                } else {
                    String coords = String.format(Locale.getDefault(), "%.5f, %.5f", point.latitude, point.longitude);
                    callback.onSuccess(new com.codram.terecojo.data.model.LocationDetails(coords, "", ""));
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void calculateDistance(LatLng origin, LatLng destination, List<LatLng> waypoints, MapCallback<Double> callback) {
        executor.execute(() -> {
            try {
                StringBuilder urlBuilder = new StringBuilder("https://maps.googleapis.com/maps/api/directions/json?");
                urlBuilder.append("origin=").append(origin.latitude).append(",").append(origin.longitude);
                urlBuilder.append("&destination=").append(destination.latitude).append(",").append(destination.longitude);

                if (waypoints != null && !waypoints.isEmpty()) {
                    urlBuilder.append("&waypoints=");
                    for (int i = 0; i < waypoints.size(); i++) {
                        LatLng p = waypoints.get(i);
                        if (p != null) {
                            urlBuilder.append(p.latitude).append(",").append(p.longitude);
                            if (i < waypoints.size() - 1) urlBuilder.append("|");
                        }
                    }
                }
                urlBuilder.append("&key=").append(API_KEY);

                HttpURLConnection conn = (HttpURLConnection) new URL(urlBuilder.toString()).openConnection();
                InputStream is = conn.getInputStream();
                String json = new String(readAllBytes(is)); // ✅ corregido
                is.close();

                JSONObject obj = new JSONObject(json);
                if (obj.getString("status").equals("OK")) {
                    JSONArray legs = obj.getJSONArray("routes").getJSONObject(0).getJSONArray("legs");
                    long totalDistanceMeters = 0;
                    for (int i = 0; i < legs.length(); i++) {
                        totalDistanceMeters += legs.getJSONObject(i).getJSONObject("distance").getLong("value");
                    }
                    callback.onSuccess(totalDistanceMeters / 1000.0);
                } else {
                    callback.onError(obj.getString("status"));
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void getDirections(LatLng origin, LatLng destination, List<LatLng> waypoints, MapCallback<String> callback) {
        executor.execute(() -> {
            try {
                StringBuilder urlBuilder = new StringBuilder("https://maps.googleapis.com/maps/api/directions/json?");
                urlBuilder.append("origin=").append(origin.latitude).append(",").append(origin.longitude);
                urlBuilder.append("&destination=").append(destination.latitude).append(",").append(destination.longitude);

                if (waypoints != null && !waypoints.isEmpty()) {
                    urlBuilder.append("&waypoints=");
                    for (int i = 0; i < waypoints.size(); i++) {
                        LatLng p = waypoints.get(i);
                        if (p != null) {
                            urlBuilder.append(p.latitude).append(",").append(p.longitude);
                            if (i < waypoints.size() - 1) urlBuilder.append("|");
                        }
                    }
                }
                urlBuilder.append("&key=").append(API_KEY);

                HttpURLConnection conn = (HttpURLConnection) new URL(urlBuilder.toString()).openConnection();
                InputStream is = conn.getInputStream();
                String json = new String(readAllBytes(is)); // ✅ corregido
                is.close();

                JSONObject obj = new JSONObject(json);
                if (obj.getString("status").equals("OK")) {
                    String points = obj.getJSONArray("routes").getJSONObject(0)
                            .getJSONObject("overview_polyline").getString("points");
                    callback.onSuccess(points);
                } else {
                    callback.onError(obj.getString("status"));
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }
}