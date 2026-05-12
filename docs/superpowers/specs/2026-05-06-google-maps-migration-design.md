# Design Spec: Google Maps Migration and UX Enhancements

Migrate the "TeRecojo" Android application from OpenStreetMap (osmdroid) to Google Maps Platform to provide a more professional experience and improve location selection accuracy.

## Goals
- Replace `osmdroid` with Google Maps SDK for Android.
- Implement a "Fixed Central Pin" location selection pattern.
- Integrate Google Places Autocomplete for address searching.
- Automate address discovery via Reverse Geocoding when moving the map.

## Architecture Changes

### 1. Dependencies
- **Remove:** `org.osmdroid:osmdroid-android`
- **Add:** 
    - `com.google.android.gms:play-services-maps`: For the map rendering.
    - `com.google.android.libraries.places:places`: For Autocomplete and Geocoding features.

### 2. Base Activity Refactoring
- Replace `MapView` with `SupportMapFragment`.
- Centralize `GoogleMap` object initialization.
- Implement `OnMapReadyCallback`.

### 3. UI Components (Layouts)
- **Central Pin:** Add a static `ImageView` (marker icon) centered over the Map fragment in `activity_main.xml`, `activity_driver.xml`, and `activity_admin.xml`.
- **Bottom Sheet:** Adjust buttons to support a "Selection Mode" where the main action button changes from "Publish" to "Confirm Location".

## Logic and Data Flow

### 1. Location Selection (The "Uber" Pattern)
- The user enters "Selection Mode" (e.g., clicking the map icon next to the Origin field).
- As the user pans/zooms the map, the central pin remains fixed.
- **Event:** `onCameraIdle()` is triggered when the map stops moving.
- **Action:** 
    1. Get coordinates from `googleMap.getCameraPosition().target`.
    2. Execute `Geocoder.getFromLocation()` (Reverse Geocoding) to get a human-readable address.
    3. Update the corresponding text field (Origin/Destination/Stop) with the address.
    4. Enable the "Confirm" button in the Bottom Sheet.

### 2. Google Places Autocomplete
- When the user taps an input field (Origin/Destination), launch the `Autocomplete.IntentBuilder`.
- On result:
    1. Move the map camera to the selected place's coordinates.
    2. The `onCameraIdle()` logic will naturally take over to refresh the precise address and marker.

### 3. Data Storage
- Continue storing `lat`, `lng`, and `description` (address) in the database via existing API endpoints.

## Testing Strategy
- **Manual Verification:** 
    - Verify map loads correctly with API Key.
    - Test Autocomplete results accuracy.
    - Verify Reverse Geocoding performance and fallback (e.g., no internet).
    - Ensure the "Fixed Pin" is perfectly centered.

## Success Criteria
- No remaining `osmdroid` code in the project.
- Smooth camera transitions when selecting addresses via Autocomplete.
- Accurate address retrieval when manually panning the map.
