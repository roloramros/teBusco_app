# Design Spec: Parallax Statistics Background for Login Screen

## 1. Overview
The "TeRecojo" login screen will feature a dynamic, minimalist background consisting of platform statistics moving in a parallax effect. This replaces the previous card-based design with a more integrated, "alive" feel where data flows behind the login form.

## 2. Visual Design
### 2.1 Background Animation
- **Style:** Multiple small text labels moving vertically (up or down).
- **Parallax Effect:** Achieved by assigning different speeds and directions to different text labels.
- **Asymmetry:** Random horizontal positions and random animation start delays.
- **Subtlety:** Low opacity (Alpha: 0.05 - 0.15) to ensure it stays in the background and doesn't interfere with the login form readability.

### 2.2 Statistics Content
The following data points will be displayed as sliding text:
1. "Numero de pasajeros activos: {count}"
2. "Número de choferes activos: {count}"
3. "Viajes Completados: {count}"
4. "Solicitudes Activas: {count}"

## 3. Technical Architecture
### 3.1 Backend (Node.js/Express)
- **Endpoint:** `GET /api/geo/stats` (Public, already implemented).
- **Response Format:**
  ```json
  {
    "ok": true,
    "data": {
      "total_usuarios": 1250,
      "total_choferes": 450,
      "viajes_completados": 3000,
      "viajes_activos": 12
    }
  }
  ```

### 3.2 Android (Java)
- **Container:** A `FrameLayout` as the root of `activity_login.xml`.
- **Layering:**
    - Layer 1 (Bottom): `vBackgroundContainer` (a `ConstraintLayout` to hold the floating texts).
    - Layer 2 (Middle): The existing `ScrollView` containing the login form (background set to transparent).
- **`ParallaxStatsManager` Utility:**
    - Responsibility: Create, animate, and update the text labels.
    - Animation: Use `ObjectAnimator.ofFloat(view, "translationY", start, end)` in a loop.
    - Reset Logic: When a text reaches the top/bottom boundary, reset its position to the opposite side with a new random X position.
- **Data Integration:**
    - Fetch stats in `LoginActivity`.
    - Pass data to `ParallaxStatsManager` to update all active labels.

## 4. Implementation Steps
### 4.1 Layout Update
- Modify `activity_login.xml`:
    - Ensure the `vBackground` (now `vBackgroundContainer`) is a layout that can hold multiple children.
    - Verify `ScrollView` background is `@android:color/transparent`.

### 4.2 Utility Class
- Create `ParallaxStatsManager.java`:
    - Method `init(ViewGroup container, List<String> templates)`
    - Method `updateValues(StatsResponse stats)`
    - Logic for spawning ~12 text views with randomized properties.

### 4.3 Integration
- Update `LoginActivity.java`:
    - Remove old card-based references.
    - Initialize `ParallaxStatsManager`.
    - Call `updateValues` when API response is received.

## 5. Success Criteria
- [x] Background shows moving text labels immediately on app start.
- [x] Text labels have varying speeds and directions (some up, some down).
- [x] Text labels are positioned behind the login form and are subtle (low alpha).
- [x] Real-time numbers from the API are correctly reflected in the labels.
