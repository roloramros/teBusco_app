# Road Distance Measurement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement trip distance calculation using Google Directions API, triggered by a manual button.

**Architecture:** 
1. Add distance display and calculation button to `activity_main.xml`.
2. Implement `calculateRoadDistance()` in `MainActivity.java` to perform background HTTP requests to Google Directions API.
3. Parse the API response to aggregate distance from all route legs (origin to destination through stops).
4. Update UI with the calculated distance in kilometers.

**Tech Stack:** Android, Java, Google Directions API, View Binding.

---

### Task 1: UI Implementation

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`

- [ ] **Step 1: Add Distance UI components**
Add the following `LinearLayout` above the "Oferta Económica" block in `app/src/main/res/layout/activity_main.xml`.

```xml
                <!-- Medición de Distancia -->
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:layout_marginTop="16dp"
                    android:gravity="center_vertical">

                    <TextView
                        android:id="@+id/tvDistance"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="Distancia: -- km"
                        android:textColor="@color/text_secondary"
                        android:textSize="14sp" />

                    <ProgressBar
                        android:id="@+id/pbDistance"
                        android:layout_width="20dp"
                        android:layout_height="20dp"
                        android:layout_marginEnd="8dp"
                        android:visibility="gone" />

                    <com.google.android.material.button.MaterialButton
                        android:id="@+id/btnCalculateDistance"
                        style="@style/Widget.Material3.Button.TextButton"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="CALCULAR RUTA"
                        android:textSize="12sp"
                        android:textColor="@color/primary_blue" />
                </LinearLayout>
```

- [ ] **Step 2: Commit UI changes**
```bash
git add app/src/main/res/layout/activity_main.xml
git commit -m "ui: add distance display and calculate button to activity_main"
```

### Task 2: Implement Distance Calculation Logic

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/MainActivity.java`

- [ ] **Step 1: Setup button listener**
In `onCreate` (via `setupMapInteractions` or similar), set the click listener for `btnCalculateDistance`.

- [ ] **Step 2: Implement `calculateRoadDistance()` method**
Implement the method to build the API URL and perform the network request.

```java
    private void calculateRoadDistance() {
        if (originPoint == null || destinationPoint == null) {
            Toast.makeText(this, "Selecciona origen y destino primero", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.pbDistance.setVisibility(View.VISIBLE);
        binding.btnCalculateDistance.setEnabled(false);

        new Thread(() -> {
            try {
                StringBuilder urlBuilder = new StringBuilder("https://maps.googleapis.com/maps/api/directions/json?");
                urlBuilder.append("origin=").append(originPoint.latitude).append(",").append(originPoint.longitude);
                urlBuilder.append("&destination=").append(destinationPoint.latitude).append(",").append(destinationPoint.longitude);
                
                if (!stopPoints.isEmpty()) {
                    urlBuilder.append("&waypoints=");
                    for (int i = 0; i < stopPoints.size(); i++) {
                        LatLng p = stopPoints.get(i);
                        if (p != null) {
                            if (i > 0) urlBuilder.append("|");
                            urlBuilder.append(p.latitude).append(",").append(p.longitude);
                        }
                    }
                }
                
                urlBuilder.append("&key=").append("AIzaSyBufiSwuBW19JLsbXKDbW86pg_1wL7ifxU"); // Key actual

                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(urlBuilder.toString()).openConnection();
                conn.setRequestMethod("GET");
                
                java.io.InputStream is = conn.getInputStream();
                String json = new String(is.readAllBytes());
                is.close();

                org.json.JSONObject obj = new org.json.JSONObject(json);
                String status = obj.getString("status");

                if (status.equals("OK")) {
                    org.json.JSONArray routes = obj.getJSONArray("routes");
                    org.json.JSONArray legs = routes.getJSONObject(0).getJSONArray("legs");
                    
                    long totalDistanceMeters = 0;
                    for (int i = 0; i < legs.length(); i++) {
                        totalDistanceMeters += legs.getJSONObject(i).getJSONObject("distance").getLong("value");
                    }

                    final double distanceKm = totalDistanceMeters / 1000.0;
                    runOnUiThread(() -> {
                        binding.tvDistance.setText(String.format(Locale.getDefault(), "Distancia: %.1f km", distanceKm));
                        binding.pbDistance.setVisibility(View.GONE);
                        binding.btnCalculateDistance.setEnabled(true);
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error: " + status, Toast.LENGTH_SHORT).show();
                        binding.pbDistance.setVisibility(View.GONE);
                        binding.btnCalculateDistance.setEnabled(true);
                    });
                }

            } catch (Exception e) {
                Log.e("Distance", "Error calculating distance", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error al calcular distancia", Toast.LENGTH_SHORT).show();
                    binding.pbDistance.setVisibility(View.GONE);
                    binding.btnCalculateDistance.setEnabled(true);
                });
            }
        }).start();
    }
```

- [ ] **Step 3: Reset distance display in `clearFields`**
Ensure `tvDistance` is reset to "Distancia: -- km" when fields are cleared.

- [ ] **Step 4: Commit logic changes**
```bash
git add app/src/main/java/com/codram/terecojo/MainActivity.java
git commit -m "feat: implement road distance calculation using Google Directions API"
```

### Task 3: Verification

- [ ] **Step 1: Verify distance calculation**
1. Select an Origin and Destination.
2. Add a Stop.
3. Tap "CALCULAR RUTA".
4. Verify the distance is displayed correctly (e.g., "Distancia: 5.2 km").
5. Change a point and recalculate to ensure it updates.

- [ ] **Step 2: Final Commit**
```bash
git commit --allow-empty -m "vibe: road distance measurement verified"
```
