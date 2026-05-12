# Authentication Enhancements Design

**Date:** 2026-05-05
**Topic:** Dual-method Login and "Remember Me" Functionality

## 1. Overview
Enhance the current authentication system to allow users to log in using their username, email, or phone number. Additionally, implement "Remember Me" functionality in the Android app to keep users logged in across sessions using token persistence.

## 2. Goals
- Update backend login logic to support username, email, or phone number as an identifier.
- Set token expiration to 24 hours.
- Implement token storage in the Android app using `SharedPreferences`.
- Implement automatic login validation on app startup.
- Add an API Interceptor in the Android app to handle the Authorization header automatically.

## 3. Backend Changes (Node.js API)

### 3.1. authController.js
- **Login Query:** Modify the SQL query in the `login` function to check the `username` column in addition to `email` and `telefono`.
- **Token Expiration:** Update the expiration time for generated tokens to 24 hours.

## 4. Frontend Changes (Android App)

### 4.1. Data Models
- Update `AuthResponse` or create a new model if needed to match the user object returned by `/api/auth/me`.

### 4.2. Session Management (`SessionManager.java`)
- Create a utility class to handle `SharedPreferences`.
- **Methods:**
    - `saveToken(String token)`
    - `getToken()`
    - `clearSession()`
    - `saveUser(User user)`
    - `getUser()`

### 4.3. API Client (`RetrofitClient.java` & `ApiService.java`)
- **Interceptor:** Add an `OkHttpClient` interceptor to `RetrofitClient` to inject the `Authorization: Bearer <token>` header into every request.
- **ApiService:** Add a `getMe()` method corresponding to the `/api/auth/me` endpoint.

### 4.4. Login Logic (`LoginActivity.java`)
- **Auto-login Check:** In `onCreate`, check if a token exists. If so, call `getMe()`.
- **Remember Me Logic:**
    - If `cbRememberMe` is checked during login, save the token to `SessionManager`.
    - If successful auto-login or regular login, navigate to the appropriate activity (`MainActivity`, `DriverActivity`, or `AdminActivity`).

## 5. Security Considerations
- Tokens are stored in private `SharedPreferences`, which is reasonably secure for basic needs but can be accessed on rooted devices.
- The `Authorization` header will be sent over HTTPS in production (ensure `usesCleartextTraffic` is managed correctly for production).

## 6. Testing Strategy
- **Backend:** Test login with username, then email, then phone.
- **Frontend:**
    - Verify token is saved when "Remember Me" is checked.
    - Verify app skips login screen on restart if token is valid.
    - Verify app stays on login screen if token is invalid or expired.
    - Verify manual logout clears the token.
