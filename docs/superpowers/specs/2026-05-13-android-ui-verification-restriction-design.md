# Design Doc: Disable "Offer" Button for Unverified Drivers - Te Busco Android

## Overview
Currently, the backend restricts unverified drivers from sending offers, but the Android UI still shows the "OFERTAR" button as enabled. This leads to a `403 Forbidden` error when an unverified driver attempts to bid. This design aims to improve user experience by disabling or hiding the "OFERTAR" button for unverified drivers.

## Proposed Changes

### 1. Data Model Update
Add the `verificado` field to the `AuthResponse.User` class to capture the user's verification status from the API.

- **File:** `app/src/main/java/com/codram/terecojo/data/model/AuthResponse.java`
- **Change:** Add `private boolean verificado;` and `public boolean isVerificado() { return verificado; }`.

### 2. UI Adapter Logic
Update `RideRequestAdapter` to handle the verification status. Since the adapter doesn't currently know about the current user's status, we have two options:
1. Pass the verification status to the adapter constructor.
2. Use `SessionManager` inside the adapter (less ideal for testability but faster).

**Recommended Approach:** Pass the status to the adapter.

- **File:** `app/src/main/java/com/codram/terecojo/ui/adapter/RideRequestAdapter.java`
- **Change:**
  - Add `private boolean isUserVerified;` field.
  - Update constructor to accept `isUserVerified`.
  - In `onBindViewHolder`, if `!isUserVerified`, disable `btnAccept`, set alpha to 0.5f, and change text to "BLOQUEADO" or "VERIFICACIÓN PENDIENTE".

### 3. Activity Updates
Pass the verification status from the `SessionManager` to the adapter in the activities that use it.

- **Files:**
  - `app/src/main/java/com/codram/terecojo/DriverActivity.java`
  - `app/src/main/java/com/codram/terecojo/DriverProfileActivity.java`
- **Change:**
  - Get the user from `SessionManager.getInstance(this).getUser()`.
  - Pass `user.isVerificado()` when creating the `RideRequestAdapter`.
  - Also, as a second layer of defense, update `onAccept(RideRequest request)` in these activities to check `user.isVerificado()` and show a Toast instead of opening the dialog if they are not verified.

### 4. Map View (Markers)
If the map also has an "Offer" button (often inside a snippet or a custom info window), it should also be disabled.

- **File:** `app/src/main/java/com/codram/terecojo/DriverActivity.java`
- **Change:** Update the logic that shows the "OFERTAR" button in the map snippet or info window to check `user.isVerificado()`.

## Implementation Details
- Ensure `SessionManager` is correctly saving the `verificado` boolean (GSON should handle it automatically if the field is added to the model).
- The button text for unverified users will be "VERIFICACIÓN PENDIENTE" to be clear about why it's disabled.

## Testing Strategy
1. **Unverified Driver:**
   - Log in.
   - Go to Radar (DriverActivity) or Profile (DriverProfileActivity).
   - Verify the "OFERTAR" button is disabled and shows "VERIFICACIÓN PENDIENTE".
   - Verify that clicking it does nothing (or shows a Toast explaining the status).
2. **Verified Driver:**
   - Log in.
   - Verify the "OFERTAR" button is enabled and works as expected.
