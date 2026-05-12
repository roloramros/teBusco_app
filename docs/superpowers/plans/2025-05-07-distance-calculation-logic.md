# Distance Calculation Logic Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement road distance calculation using Google Directions API and clean up hardcoded UI strings.

**Architecture:** Use `HttpURLConnection` in a background thread to fetch distance from Google Directions API, parsing JSON response to display total distance in kilometers.

**Tech Stack:** Java, Android SDK, Google Directions API, View Binding.

---

### Task 1: UI String Externalization

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/layout/activity_main.xml`

- [ ] **Step 1: Add new strings to `strings.xml`**

```xml
    <string name="label_distance_default">Distancia: -- km</string>
    <string name="label_distance_format">Distancia: %.1f km</string>
    <string name="btn_calculate_route">CALCULAR RUTA</string>
    <string name="msg_error_distance">Error al calcular distancia</string>
    <string name="msg_select_points_first">Selecciona origen y destino primero</string>
```

- [ ] **Step 2: Update `activity_main.xml` to use string resources**

Replace hardcoded strings for `tvDistance` and `btnCalculateDistance`.

- [ ] **Step 3: Commit UI string changes**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/layout/activity_main.xml
git commit -m "ui: externalize distance calculation strings"
```

### Task 2: Distance Calculation Implementation

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/MainActivity.java`

- [ ] **Step 1: Set up button listener in `setupMapInteractions`**

```java
    private void setupMapInteractions() {
        binding.etOrigin.setOnClickListener(v -> startMapSelection(SelectionMode.ORIGIN, -1));
        binding.etDestination.setOnClickListener(v -> startMapSelection(SelectionMode.DESTINATION, -1));
        binding.btnCalculateDistance.setOnClickListener(v -> calculateRoadDistance());
    }
```

- [ ] **Step 2: Implement `calculateRoadDistance()` method**

Add the method to perform the API request and handle the response.

```java
    private void calculateRoadDistance() {
        if (originPoint == null || destinationPoint == null) {
            Toast.makeText(this, R.string.msg_select_points_first, Toast.LENGTH_SHORT).show();
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
                    boolean hasWaypoints = false;
                    for (LatLng p : stopPoints) {
                        if (p != null) {
                            if (!hasWaypoints) {
                                urlBuilder.append("&waypoints=");
                                hasWaypoints = true;
                            } else {
                                urlBuilder.append("|");
                            }
                            urlBuilder.append(p.latitude).append(",").append(p.longitude);
                        }
                    }
                }
                
                urlBuilder.append("&key=").append("AIzaSyBufiSwuBW19JLsbXKDbW86pg_1wL7ifxU");

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
                        binding.tvDistance.setText(getString(R.string.label_distance_format, distanceKm));
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
                    Toast.makeText(this, R.string.msg_error_distance, Toast.LENGTH_SHORT).show();
                    binding.pbDistance.setVisibility(View.GONE);
                    binding.btnCalculateDistance.setEnabled(true);
                });
            }
        }).start();
    }
```

- [ ] **Step 3: Reset distance display in `clearFields`**

```java
    private void clearFields() {
        // ... existing code ...
        binding.tvDistance.setText(R.string.label_distance_default);
        // ... rest of existing code ...
    }
```

- [ ] **Step 4: Verify Compilation**

Run `./gradlew assembleDebug` to ensure there are no compilation errors.

- [ ] **Step 5: Commit logic changes**

```bash
git add app/src/main/java/com/codram/terecojo/MainActivity.java
git commit -m "feat: implement road distance calculation using Google Directions API"
```
