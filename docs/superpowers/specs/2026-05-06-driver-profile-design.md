# Design Spec: Driver Profile Implementation

## 1. Overview
The goal is to implement a dedicated profile screen for drivers in the TeRecojo app. This screen will serve as the driver's home, displaying their stats and pending requests. It will also provide access to the map and a way to add vehicles.

## 2. Components

### 2.1 `DriverProfileActivity`
- **Purpose**: Main landing page for drivers.
- **Layout Structure**:
    - `DrawerLayout` as the root.
    - `CoordinatorLayout` to host the content and FAB.
    - `MaterialToolbar` with a hamburger icon for the drawer.
    - **Header (Stats Grid)**: Fixed grid at the top showing Trips, Rating, and Earnings.
    - **List (Requests)**: A `RecyclerView` below the stats showing pending ride requests.
    - **FAB**: A Floating Action Button with a map icon to navigate to `DriverActivity` (Map view).

### 2.2 `AddVehicleDialogFragment`
- **Purpose**: Modal to collect vehicle information.
- **Fields**:
    - Brand (Marca)
    - Model (Modelo)
    - License Plate (Placa/Matrícula)
    - Color
- **Actions**: "Cancel" and "Save".

### 2.3 Navigation Drawer
- **Location**: `DriverProfileActivity`.
- **Items**:
    - Profile (Current)
    - Add Vehicle (Triggers Dialog)
    - Logout (Existing functionality)

## 3. Data Flow
- **Login**: If the user role is "driver", redirect to `DriverProfileActivity` instead of `MainActivity`.
- **Requests**: Initially populated with mock data in a `PendingRequest` model.
- **Vehicle Addition**: UI-only for now (as requested), using a Dialog.

## 4. UI/UX Details
- **Stats**: Use `MaterialCardView` for a modern, clean look.
- **Requests**: List items will show origin, destination, and passenger name.
- **FAB**: Standard Material FAB in the bottom right corner.

## 5. Proposed Changes
1. Create `DriverProfileActivity.java` and `activity_driver_profile.xml`.
2. Create `AddVehicleDialogFragment.java` and `dialog_add_vehicle.xml`.
3. Create a `PendingRequest` model and a `RequestAdapter`.
4. Update `LoginActivity.java` to handle redirection.
5. Update navigation drawer logic in `BaseActivity` or the new activity to handle "Add Vehicle".
