# Login and Registration Implementation Design

**Date:** 2026-05-04
**Topic:** Login and Registration Activities for TeRecojo

## 1. Overview
Implementation of two Android Activities (`LoginActivity` and `RegisterActivity`) using Java and Material Design 3. The focus is on UI layout and basic navigation between the two screens.

## 2. Goals
- Create a modern, visually appealing Login screen.
- Create a comprehensive Registration form with various input types.
- Use the provided application icon (`icono.png`) throughout the app.
- Implement the "User Type" selector based on the provided reference (`guia tipo user.png`).
- Enable ViewBinding for clean Java code.
- Implement simple navigation (Intents) between activities.

## 3. Technical Stack
- **Language:** Java 11
- **UI:** XML with Material Design 3 components.
- **Tools:** ViewBinding.
- **Target SDK:** 36 (as per `build.gradle.kts`).

## 4. Assets
- `icono.png`: To be used as the app icon and as a logo in `LoginActivity`.
- `guia tipo user.png`: Used as a reference for the Passenger/Driver toggle in `RegisterActivity`.

## 5. UI Components

### 5.1. LoginActivity
- **Logo:** `ImageView` displaying `icono.png`.
- **Username:** `TextInputLayout` + `TextInputEditText`.
- **Password:** `TextInputLayout` + `TextInputEditText` (Password type with toggle).
- **Remember Me:** `MaterialCheckBox` ("Mantener logueado").
- **Login Button:** `MaterialButton` ("Entrar").
- **Register Link:** `TextView` ("¿No tienes cuenta? Regístrate") that navigates to `RegisterActivity`.

### 5.2. RegisterActivity
- **Scroll View:** `NestedScrollView` to ensure all fields are accessible on smaller screens.
- **Full Name:** `TextInputLayout` + `TextInputEditText`.
- **Username:** `TextInputLayout` + `TextInputEditText`.
- **Phone:** `TextInputLayout` + `TextInputEditText` (Phone type).
- **Password:** `TextInputLayout` + `TextInputEditText` (Password type).
- **Province:** `MaterialSpinner` (Placeholder for later population).
- **Municipality:** `MaterialSpinner` (Placeholder for later population).
- **User Type Selector:** 
    - A `MaterialButtonToggleGroup` containing two buttons: "Pasajero" and "Conductor".
    - Logic to handle visual state changes when a button is selected.
- **Register Button:** `MaterialButton` ("Registrar").
- **Back to Login:** Navigation back to `LoginActivity`.

## 6. Architecture & Implementation
- **ViewBinding:** Will be enabled in `app/build.gradle.kts`.
- **Navigation:** Standard `Intent` usage. No complex navigation component for this simple flow.
- **Styles:** Use `Theme.TeRecojo` (Material 3) as the base theme.

## 7. Testing Strategy
- Manual verification of layout rendering on different screen sizes.
- Verify click listeners for navigation.
- Verify toggle behavior in the User Type selector.
