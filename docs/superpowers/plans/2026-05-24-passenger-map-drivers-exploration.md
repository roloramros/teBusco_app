# Passenger Map Driver Exploration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an exploration mode to `MainActivity` that displays nearby available drivers on the map, with a BottomSheet showing driver details when a marker is tapped.

**Architecture:** Use a toggle state in `MainActivity` to switch between "Publication Mode" (current) and "Exploration Mode" (new). Exploration mode fetches drivers from a new API endpoint and manages its own set of map markers and a dedicated BottomSheet.

**Tech Stack:** Java, Android SDK, Google Maps SDK, Retrofit, Material Design Components.

---

### Task 1: Data Model and API Service

**Files:**
- Create: `app/src/main/java/com/codram/terecojo/data/model/ChoferDisponible.java`
- Modify: `app/src/main/java/com/codram/terecojo/data/remote/ApiService.java`

- [ ] **Step 1: Create ChoferDisponible model**
Create the file with the specified fields and getters.

- [ ] **Step 2: Add endpoint to ApiService**
Add `getChoferesDisponibles` to `ApiService.java`.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/codram/terecojo/data/model/ChoferDisponible.java app/src/main/java/com/codram/terecojo/data/remote/ApiService.java
git commit -m "feat: add ChoferDisponible model and API endpoint"
```

### Task 2: UI Resources

**Files:**
- Create: `app/src/main/res/drawable/bg_tag_blue.xml`
- Create: `app/src/main/res/drawable/bg_bottom_sheet_handle.xml`
- Create: `app/src/main/res/layout/bottom_sheet_chofer.xml`

- [ ] **Step 1: Create bg_tag_blue.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#E3F0FF" />
    <corners android:radius="20dp" />
</shape>
```

- [ ] **Step 2: Create bg_bottom_sheet_handle.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#E0E0E0" />
    <corners android:radius="2dp" />
</shape>
```

- [ ] **Step 3: Create bottom_sheet_chofer.xml**
Use the layout provided in the prompt.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/res/drawable/bg_tag_blue.xml app/src/main/res/drawable/bg_bottom_sheet_handle.xml app/src/main/res/layout/bottom_sheet_chofer.xml
git commit -m "feat: add UI resources for driver exploration bottom sheet"
```

### Task 3: Layout Update

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`

- [ ] **Step 1: Add fabExplorar to activity_main.xml**
Add the FloatingActionButton inside the `main_content` container.

- [ ] **Step 2: Commit**
```bash
git add app/src/main/res/layout/activity_main.xml
git commit -m "feat: add fabExplorar to activity_main layout"
```

### Task 4: MainActivity Logic

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/MainActivity.java`

- [ ] **Step 1: Add imports and instance variables**
Add `ChoferDisponible` import and the required state variables.

- [ ] **Step 2: Initialize exploration mode**
Call `setupModoExploracion()` in `onCreate` and implement the method.

- [ ] **Step 3: Implement toggle logic**
Implement `activarModoExploracion()` and `desactivarModoExploracion()`.

- [ ] **Step 4: Implement data fetching and display**
Implement `cargarChoferesDisponibles()` and `mostrarInfoChofer()`.

- [ ] **Step 5: Set up marker click listener**
In `onMapReady`, add the listener to handle clicks on driver markers.

- [ ] **Step 6: Commit**
```bash
git add app/src/main/java/com/codram/terecojo/MainActivity.java
git commit -m "feat: implement driver exploration logic in MainActivity"
```

### Task 5: Verification

- [ ] **Step 1: Build the project**
Run: `./gradlew assembleDebug`
Expected: SUCCESSFUL build.

- [ ] **Step 2: Verify logic (Manual)**
The changes are extensive and visual. Verify that the exploration mode correctly toggles visibility of elements and fetches data when activated.
