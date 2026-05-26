# Spec: Mi Licencia View for Driver App

## Status
- **Topic**: Driver License management interface in the mobile app.
- **Approved Approach**: Approach 1 (Dashboard style with status header, circular progress for remaining days, and balance card).
- **Date**: 2026-05-25

## 1. Overview
The "Mi Licencia" view allows drivers to monitor their legal and financial standing within the app. It provides real-time information about their trial or subscription status, remaining days, current balance, and monthly quota.

## 2. Architecture & UI Structure

### Layout: `activity_mi_licencia.xml`
- **Root**: `DrawerLayout` (to maintain sidebar navigation consistency).
- **Header**: `MaterialToolbar` with the title "Mi Licencia".
- **Content Container**: `ScrollView` (to handle different screen sizes).
    - **Status Card**: A prominent card at the top.
        - Dynamic background: Blue (`#1E88E5`) for Trial, Green (`#4CAF50`) for Active, Red (`#E53935`) for Suspended.
        - Dynamic text: Large label showing "Trial Activo", "Licencia Activa", or "Licencia Suspendida".
    - **Time Indicator**:
        - `CircularProgressBar` (centered).
        - Interior text: "X días restantes" or "Vencida".
        - Fullness: Percentage calculated as `(dias_restantes / total_periodo) * 100`.
    - **Balance Card ("Tu Fondo")**:
        - Large display of `saldo_fondo` (formatted with `$`).
        - Label showing the `monto_mensual`.
    - **Actions**:
        - Button: "¿Cómo recargar?" (Material Button, outlined style).
    - **Renewal Info**:
        - Small footer text: "Próxima renovación: DD/MM/AAAA".

## 3. Data Flow
- **Endpoint**: `GET /api/auth/mi-licencia`.
- **Response Handling**:
    - Map `estado` to UI colors and labels.
    - Calculate percentage for the circular progress bar.
    - Format dates (`trial_fin` or `suscripcion_fin`) for display.
- **Legacy Support**: If no license is found (status `SIN_LICENCIA`), display a specific message encouraging the user to contact support for activation.

## 4. Interaction Logic
- **Recargar Button**: Triggers a `MaterialAlertDialogBuilder` with static instructions (to be defined by the user later).
- **Swipe-to-Refresh**: Wrap the content in a `SwipeRefreshLayout` to allow manual data updates.

## 5. Visual Specifications (Colors)
- **Trial**: `primary_blue` (`#1E88E5`).
- **Active**: `success_green` (`#4CAF50`).
- **Suspended**: `error_red` (`#E53935`).
- **Background**: `background_light` (`#F5F7FA`).

## 6. Testing & Validation
- **UI Check**: Verify that the header color and text change correctly based on the API response.
- **Data Binding**: Ensure saldo and quota match the values set in the admin panel.
- **Offline Mode**: Display a "No internet" message if the request fails.
