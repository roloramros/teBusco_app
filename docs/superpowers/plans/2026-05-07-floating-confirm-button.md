# Floating Location Confirmation FAB Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the dual-purpose "Publish" button with a dedicated Floating Action Button for location confirmation, ensuring the Bottom Sheet expands automatically after selection.

**Architecture:** 
1. Add an `ExtendedFloatingActionButton` to `activity_main.xml` positioned above the Bottom Sheet.
2. Update `MainActivity.java` to show the FAB during location selection and hide it after confirmation.
3. Redirect `btnPublishRequest` to strictly handle trip publication.
4. Ensure the Bottom Sheet expands to `STATE_EXPANDED` upon confirming a location.

**Tech Stack:** Android, Java, Material Components, View Binding.

---

### Task 1: UI Layout Update

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add new string for FAB**
Update `app/src/main/res/values/strings.xml`:
```xml
<string name="btn_confirm_point">Confirmar Punto</string>
```

- [ ] **Step 2: Add ExtendedFloatingActionButton to layout**
Modify `app/src/main/res/layout/activity_main.xml` to include the FAB inside the `CoordinatorLayout`, above the `bottomSheet`.

```xml
        <!-- FAB de Confirmar Ubicación (Se muestra solo al seleccionar) -->
        <com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
            android:id="@+id/fabConfirmLocation"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_margin="16dp"
            android:layout_gravity="bottom|end"
            android:layout_marginBottom="140dp"
            android:text="@string/btn_confirm_point"
            android:textColor="@color/white"
            app:icon="@android:drawable/ic_menu_save"
            app:iconTint="@color/white"
            app:backgroundTint="@color/primary_blue"
            android:visibility="gone" />
```
*Note: Ensure `layout_marginBottom` is higher than `fabMyLocation` or appropriately positioned above the collapsed Bottom Sheet.*

- [ ] **Step 3: Commit UI changes**
```bash
git add app/src/main/res/layout/activity_main.xml app/src/main/res/values/strings.xml
git commit -m "ui: add fabConfirmLocation to activity_main"
```

### Task 2: Update Selection Logic in MainActivity

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/MainActivity.java`

- [ ] **Step 1: Update `startMapSelection` to show FAB**
Modify `startMapSelection` to show `fabConfirmLocation`, set its click listener, and stop modifying `btnPublishRequest`.

```java
    private void startMapSelection(SelectionMode mode, int stopIndex) {
        currentSelectionMode = mode;
        currentlyEditingStopIndex = stopIndex;
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        binding.ivCentralPin.setVisibility(View.VISIBLE);
        
        // Mostrar FAB de confirmación y ocultar botón de publicar
        binding.fabConfirmLocation.setVisibility(View.VISIBLE);
        binding.fabConfirmLocation.setOnClickListener(v -> confirmLocationSelection());
        binding.btnPublishRequest.setVisibility(View.GONE);
        
        Toast.makeText(this, R.string.msg_selecting_location, Toast.LENGTH_SHORT).show();
    }
```

- [ ] **Step 2: Update `confirmLocationSelection` to restore UI state**
Modify `confirmLocationSelection` to hide FAB, show "Publish" button, and expand Bottom Sheet.

```java
    private void confirmLocationSelection() {
        if (mMap == null) return;
        LatLng center = mMap.getCameraPosition().target;
        
        SelectionMode finalizedMode = currentSelectionMode;
        int finalizedIndex = currentlyEditingStopIndex;

        Log.d("Selection", "Confirmando ubicación para " + finalizedMode);
        
        reverseGeocode(center, finalizedMode, finalizedIndex);
        updateMarker(finalizedMode, center, finalizedIndex);
        
        binding.ivCentralPin.setVisibility(View.GONE);
        binding.fabConfirmLocation.setVisibility(View.GONE); // Ocultar FAB
        binding.btnPublishRequest.setVisibility(View.VISIBLE); // Mostrar botón de publicar
        
        currentSelectionMode = SelectionMode.NONE;
        currentlyEditingStopIndex = -1;
        
        // Expandir el panel automáticamente
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }
```

- [ ] **Step 3: Remove dynamic button logic from `applyGeocodedAddress`**
Remove the block that changes `btnPublishRequest` text in `applyGeocodedAddress`.

- [ ] **Step 4: Cleanup `setupBottomSheet` and `clearFields`**
Ensure `btnPublishRequest` click listener is set once and visibility is reset in `clearFields`.

- [ ] **Step 5: Commit logic changes**
```bash
git add app/src/main/java/com/codram/terecojo/MainActivity.java
git commit -m "feat: implement dedicated location confirmation FAB and auto-expand bottom sheet"
```

### Task 3: Verification and Polish

**Files:**
- Test: Manual verification (or update `ExampleInstrumentedTest.java` if possible)

- [ ] **Step 1: Verify flow**
1. Open app.
2. Tap "Destino final".
3. Verify Bottom Sheet collapses, Pin appears, and "Confirmar Punto" FAB appears.
4. Verify "PUBLICAR VIAJE" button is hidden.
5. Move map, then tap FAB.
6. Verify Bottom Sheet expands, address is updated, FAB is hidden, and "PUBLICAR VIAJE" is visible.

- [ ] **Step 2: Final Commit**
```bash
git commit --allow-empty -m "vibe: location confirmation flow verified"
```
