# Google Maps Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace OpenStreetMap with Google Maps SDK, implement a fixed central pin for location selection, and integrate Google Places Autocomplete.

**Architecture:** Refactor `BaseActivity` to use `SupportMapFragment` instead of `MapView`. Implement a static central pin overlay in layouts. Use `onCameraIdle` for reverse geocoding and `Autocomplete.IntentBuilder` for address search.

**Tech Stack:** Android (Java), Google Maps SDK, Google Places SDK, Retrofit.

---

### Task 1: Dependency Management and API Configuration

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add Google Maps and Places to Version Catalog**

Modify `gradle/libs.versions.toml`:
```toml
[versions]
# ... existing versions
googleMaps = "18.2.0"
googlePlaces = "3.3.0"

[libraries]
# ... existing libraries
google-maps = { group = "com.google.android.gms", name = "play-services-maps", version.ref = "googleMaps" }
google-places = { group = "com.google.android.libraries.places", name = "places", version.ref = "googlePlaces" }
```

- [ ] **Step 2: Update App Dependencies**

Modify `app/build.gradle.kts`:
```kotlin
dependencies {
    // Remove
    // implementation(libs.osmdroid)
    
    // Add
    implementation(libs.google.maps)
    implementation(libs.google.places)
}
```

- [ ] **Step 3: Add API Key to AndroidManifest.xml**

Modify `app/src/main/AndroidManifest.xml`:
```xml
<application ...>
    <meta-data
        android:name="com.google.android.geo.API_KEY"
        android:value="YOUR_API_KEY_HERE" />
</application>
```

- [ ] **Step 4: Sync Gradle and Commit**

Run: `./gradlew assembleDebug`
Expected: SUCCESS

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/AndroidManifest.xml
git commit -m "build: add google maps and places dependencies"
```

---

### Task 2: Refactor BaseActivity for Google Maps

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/BaseActivity.java`

- [ ] **Step 1: Replace osmdroid imports and MapView with SupportMapFragment**

Modify `app/src/main/java/com/codram/terecojo/BaseActivity.java`:
```java
// Remove osmdroid imports
// import org.osmdroid.views.MapView; ...

// Add Google Maps imports
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;

public abstract class BaseActivity extends AppCompatActivity implements OnMapReadyCallback {
    protected GoogleMap mMap;
    // Remove protected MapView mapView;
    // Remove protected MyLocationNewOverlay mLocationOverlay;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Remove Configuration.getInstance().setUserAgentValue(getPackageName());
        
        // Initialize Places
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "YOUR_API_KEY_HERE");
        }
    }

    protected void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapView);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Default settings
        mMap.getUiSettings().setMyLocationButtonEnabled(false); // We have our own FAB
        
        LatLng cuba = new LatLng(23.1136, -82.3666);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cuba, 15f));
        
        enableMyLocation();
    }

    protected void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) mMap.setMyLocationEnabled(true);
        }
    }
}
```

- [ ] **Step 2: Update setupDrawer and FAB logic**

Modify `BaseActivity.java`'s `setupDrawer` and FAB click listener:
```java
// In setupDrawer:
// Replace mapView = findViewById(R.id.mapView); with:
setupMap();

// FAB logic update:
if (fabMyLocation != null) {
    fabMyLocation.setOnClickListener(v -> {
        if (mMap != null && mMap.getMyLocation() != null) {
            LatLng myLocation = new LatLng(mMap.getMyLocation().getLatitude(), mMap.getMyLocation().getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 17.0f));
        }
    });
}
```

- [ ] **Step 3: Remove onResume/onPause osmdroid logic**

```java
@Override
protected void onResume() {
    super.onResume();
    // Remove if (mapView != null) mapView.onResume(); ...
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/codram/terecojo/BaseActivity.java
git commit -m "refactor: migrate BaseActivity to Google Maps"
```

---

### Task 3: UI Implementation - Central Pin Overlay

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/layout/activity_driver.xml`
- Modify: `app/src/main/res/layout/activity_admin.xml`

- [ ] **Step 1: Replace MapView with FragmentContainerView and add Central Pin**

Modify `app/src/main/res/layout/activity_main.xml`:
```xml
<!-- Replace org.osmdroid.views.MapView with: -->
<androidx.fragment.app.FragmentContainerView
    android:id="@+id/mapView"
    android:name="com.google.android.gms.maps.SupportMapFragment"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />

<!-- Add this after the FragmentContainerView but inside the same FrameLayout/CoordinatorLayout -->
<ImageView
    android:id="@+id/ivCentralPin"
    android:layout_width="40dp"
    android:layout_height="40dp"
    android:layout_gravity="center"
    android:layout_marginBottom="20dp" 
    android:src="@drawable/ic_my_location" 
    android:visibility="gone" />
```
*(Repeat for `activity_driver.xml` and `activity_admin.xml`)*

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/layout/activity_main.xml app/src/main/res/layout/activity_driver.xml app/src/main/res/layout/activity_admin.xml
git commit -m "ui: add central pin overlay and replace MapView with FragmentContainerView"
```

---

### Task 4: Location Selection Logic in MainActivity

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/MainActivity.java`

- [ ] **Step 1: Replace Marker logic with Central Pin interaction**

Modify `app/src/main/java/com/codram/terecojo/MainActivity.java`:
```java
// Remove osmdroid Marker imports
// Add LatLng imports

@Override
public void onMapReady(GoogleMap googleMap) {
    super.onMapReady(googleMap);
    
    mMap.setOnCameraIdleListener(() -> {
        if (currentSelectionMode != SelectionMode.NONE) {
            LatLng center = mMap.getCameraPosition().target;
            reverseGeocode(center);
        }
    });
}

private void reverseGeocode(LatLng point) {
    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
    try {
        List<Address> addresses = geocoder.getFromLocation(point.latitude, point.longitude, 1);
        if (addresses != null && !addresses.isEmpty()) {
            String addressLine = addresses.get(0).getAddressLine(0);
            updateSelectedField(addressLine, point);
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}

private void updateSelectedField(String address, LatLng point) {
    switch (currentSelectionMode) {
        case ORIGIN:
            originPoint = new GeoPoint(point.latitude, point.longitude); // Keep GeoPoint for now if data model uses it
            binding.etOrigin.setText(address);
            break;
        case DESTINATION:
            destinationPoint = new GeoPoint(point.latitude, point.longitude);
            binding.etDestination.setText(address);
            break;
        // ... same for stops
    }
    // Update button text to "Confirmar Punto"
    binding.btnPublishRequest.setText("Confirmar Ubicación");
}
```

- [ ] **Step 2: Implement Google Places Autocomplete**

```java
private void startAutocomplete(SelectionMode mode) {
    List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS);
    Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            .setCountry("CU") // Limit to Cuba
            .build(this);
    startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
}

@Override
protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
        if (resultCode == RESULT_OK) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 17f));
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/codram/terecojo/MainActivity.java
git commit -m "feat: implement central pin logic and google places autocomplete"
```

---

### Task 5: Final Cleanup and Verification

**Files:**
- Modify: `app/build.gradle.kts`
- Remove: Unused resources/imports

- [ ] **Step 1: Remove all remaining osmdroid references**
- [ ] **Step 2: Run full build and manual verification**
- [ ] **Step 3: Commit final cleanup**

```bash
git commit -m "cleanup: remove all osmdroid traces"
```
