# Empty State Illustrations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement reusable empty state illustrations for the Radar and My Requests screens to improve UX when no data is available.

**Architecture:** Create a reusable XML layout (`layout_empty_state.xml`) and include it in target activities. Use View Binding to toggle visibility and customize content (text/icons/actions) from Java code.

**Tech Stack:** Android (Java), XML Layouts, View Binding, Material Components.

---

### Task 1: Create Reusable Empty State Layout

**Files:**
- Create: `app/src/main/res/layout/layout_empty_state.xml`

- [ ] **Step 1: Create the layout file**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/emptyStateContainer"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center"
    android:orientation="vertical"
    android:padding="32dp"
    android:visibility="gone">

    <ImageView
        android:id="@+id/ivEmptyIcon"
        android:layout_width="120dp"
        android:layout_height="120dp"
        android:layout_marginBottom="24dp"
        android:src="@android:drawable/ic_menu_search"
        app:tint="@color/text_secondary" />

    <TextView
        android:id="@+id/tvEmptyTitle"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginBottom="8dp"
        android:text="Título"
        android:textAlignment="center"
        android:textAppearance="@style/TextAppearance.Material3.TitleLarge"
        android:textStyle="bold" />

    <TextView
        android:id="@+id/tvEmptyDescription"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginBottom="32dp"
        android:text="Descripción detallada del estado vacío."
        android:textAlignment="center"
        android:textAppearance="@style/TextAppearance.Material3.BodyMedium"
        android:textColor="@color/text_secondary" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnEmptyAction"
        style="@style/Widget.Material3.Button.OutlinedButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Acción"
        app:cornerRadius="12dp" />

</LinearLayout>
```

- [ ] **Step 2: Commit layout**

```bash
git add app/src/main/res/layout/layout_empty_state.xml
git commit -m "feat: add reusable empty state layout"
```

---

### Task 2: Integrate Empty State in Driver Profile

**Files:**
- Modify: `app/src/main/res/layout/activity_driver_profile.xml`
- Modify: `app/src/main/java/com/codram/terecojo/DriverProfileActivity.java`

- [ ] **Step 1: Update activity_driver_profile.xml**
Add the `<include>` tag inside the `LinearLayout` that contains the `SwipeRefreshLayout`.

```xml
<!-- ... inside LinearLayout ... -->
<include
    android:id="@+id/layoutEmpty"
    layout="@layout/layout_empty_state" />

<androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    android:id="@+id/swipeRefreshRadar"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:layout_marginTop="8dp">
<!-- ... -->
```

- [ ] **Step 2: Update DriverProfileActivity.java**
Implement `updateEmptyState` and call it in `loadRequests`.

```java
// Inside DriverProfileActivity.java

private void updateEmptyState(boolean isEmpty) {
    if (isEmpty) {
        profileBinding.layoutEmpty.getRoot().setVisibility(View.VISIBLE);
        profileBinding.swipeRefreshRadar.setVisibility(View.GONE);
        
        profileBinding.layoutEmpty.ivEmptyIcon.setImageResource(android.R.drawable.ic_menu_compass);
        profileBinding.layoutEmpty.tvEmptyTitle.setText("Radar despejado");
        profileBinding.layoutEmpty.tvEmptyDescription.setText("No hay solicitudes de viaje en tu zona en este momento.");
        profileBinding.layoutEmpty.btnEmptyAction.setText("Actualizar");
        profileBinding.layoutEmpty.btnEmptyAction.setOnClickListener(v -> {
            profileBinding.swipeRefreshRadar.setRefreshing(true);
            loadRequests();
        });
    } else {
        profileBinding.layoutEmpty.getRoot().setVisibility(View.GONE);
        profileBinding.swipeRefreshRadar.setVisibility(View.VISIBLE);
    }
}

// In loadRequests() -> onResponse:
if (response.isSuccessful() && response.body() != null) {
    requests.clear();
    requests.addAll(response.body().getData());
    adapter.notifyDataSetChanged();
    updateEmptyState(requests.isEmpty()); // Add this
}

// In loadRequests() -> onFailure:
profileBinding.swipeRefreshRadar.setRefreshing(false);
updateEmptyState(requests.isEmpty()); // Also handle failure case if list was already empty
```

- [ ] **Step 3: Commit changes**

```bash
git add app/src/main/res/layout/activity_driver_profile.xml app/src/main/java/com/codram/terecojo/DriverProfileActivity.java
git commit -m "feat: implement empty state in Driver Profile"
```

---

### Task 3: Integrate Empty State in My Requests

**Files:**
- Modify: `app/src/main/res/layout/activity_my_requests.xml`
- Modify: `app/src/main/java/com/codram/terecojo/MyRequestsActivity.java`

- [ ] **Step 1: Update activity_my_requests.xml**
Include the layout as a sibling to `SwipeRefreshLayout`.

```xml
<!-- ... inside CoordinatorLayout ... -->
<include
    android:id="@+id/layoutEmpty"
    layout="@layout/layout_empty_state" />

<androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    android:id="@+id/swipeRefresh"
<!-- ... -->
```

- [ ] **Step 2: Update MyRequestsActivity.java**
Implement `updateEmptyState` and call it in `fetchMyRequests`.

```java
// Inside MyRequestsActivity.java

private void updateEmptyState(boolean isEmpty) {
    if (isEmpty) {
        binding.layoutEmpty.getRoot().setVisibility(View.VISIBLE);
        binding.swipeRefresh.setVisibility(View.GONE);
        
        binding.layoutEmpty.ivEmptyIcon.setImageResource(android.R.drawable.ic_menu_recent_history);
        binding.layoutEmpty.tvEmptyTitle.setText("Sin viajes aún");
        binding.layoutEmpty.tvEmptyDescription.setText("¿A dónde quieres ir? Empieza a viajar con nosotros hoy.");
        binding.layoutEmpty.btnEmptyAction.setText("Solicitar Viaje");
        binding.layoutEmpty.btnEmptyAction.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    } else {
        binding.layoutEmpty.getRoot().setVisibility(View.GONE);
        binding.swipeRefresh.setVisibility(View.VISIBLE);
    }
}

// In fetchMyRequests() -> onResponse:
if (response.isSuccessful() && response.body() != null) {
    myRequests.clear();
    myRequests.addAll(response.body().getData());
    adapter.notifyDataSetChanged();
    updateEmptyState(myRequests.isEmpty()); // Add this
}

// In fetchMyRequests() -> onFailure:
binding.swipeRefresh.setRefreshing(false);
updateEmptyState(myRequests.isEmpty());
```

- [ ] **Step 3: Commit changes**

```bash
git add app/src/main/res/layout/activity_my_requests.xml app/src/main/java/com/codram/terecojo/MyRequestsActivity.java
git commit -m "feat: implement empty state in My Requests"
```

---

### Task 4: Verification

- [ ] **Step 1: Verify Radar Empty State**
Run the app, log in as a driver, go to "Radar" (Driver Profile). Ensure that if there are no requests, the "Radar despejado" illustration appears. Click "Actualizar" and verify it triggers a refresh.

- [ ] **Step 2: Verify My Requests Empty State**
Log in as a user, go to "Mis Solicitudes". Ensure that if there are no requests, the "Sin viajes aún" illustration appears. Click "Solicitar Viaje" and verify it navigates back to the main map.
