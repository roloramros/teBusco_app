# Design Spec - Empty State Illustrations

Implementation of empty states for the "Radar" and "My Requests" screens in the Android application to improve user experience when no data is available.

## 1. Reusable Component: `layout_empty_state.xml`

A generic layout that can be customized dynamically from Java code.

### Layout Structure (LinearLayout - Vertical)
- **ImageView (`ivEmptyIcon`):** Large icon (e.g., `@android:drawable/ic_menu_search` or similar) with `@color/text_secondary` tint.
- **TextView (`tvEmptyTitle`):** Bold, large text (TextAppearance.Material3.TitleLarge).
- **TextView (`tvEmptyDescription`):** Informative text (TextAppearance.Material3.BodyMedium, centered).
- **MaterialButton (`btnEmptyAction`):** Call to action button.

## 2. Screen-Specific Configurations

### A. Radar (DriverProfileActivity)
- **Trigger:** `adapter.getItemCount() == 0` after fetching requests.
- **Title:** "Radar despejado"
- **Description:** "No hay solicitudes de viaje en tu zona en este momento."
- **Button Text:** "Actualizar"
- **Action:** Triggers `fetchRequests()` (or `swipeRefreshRadar.setRefreshing(true)`).

### B. My Requests (MyRequestsActivity)
- **Trigger:** `adapter.getItemCount() == 0` after fetching requests.
- **Title:** "Sin viajes aún"
- **Description:** "¿A dónde quieres ir? Empieza a viajar con nosotros hoy."
- **Button Text:** "Solicitar Viaje"
- **Action:** `startActivity(new Intent(this, MainActivity.class))` and `finish()`.

## 3. Implementation Logic

### XML Integration
The layout will be included using `<include />` in:
- `activity_driver_profile.xml`
- `activity_my_requests.xml`

It will be placed as a sibling to the `SwipeRefreshLayout` or `RecyclerView`, initially set to `android:visibility="gone"`.

### Java Logic
A helper method `showEmptyState(boolean show)` will be added to both activities:
```java
private void showEmptyState(boolean show) {
    binding.emptyState.getRoot().setVisibility(show ? View.VISIBLE : View.GONE);
    binding.swipeRefresh.setVisibility(show ? View.GONE : View.VISIBLE);
}
```

## 4. Assets
We will use standard Android Material icons:
- Radar: `@android:drawable/ic_menu_compass`
- History: `@android:drawable/ic_menu_recent_history` (or similar)

## 5. Verification Plan
1.  **Driver Radar:** Log in as a driver, ensure no requests are nearby, and verify the empty state appears with the "Update" button.
2.  **User History:** Log in as a user with a new account (or delete all requests), and verify the empty state appears with the "Solicit Trip" button.
3.  **Data Presence:** Ensure the empty state disappears once data is loaded.
