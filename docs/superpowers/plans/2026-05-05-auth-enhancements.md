# Authentication Enhancements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable login via username, email, or phone, and implement "Remember Me" functionality in the Android app.

**Architecture:** Backend will be updated to support multi-identifier login and 24h tokens. Frontend will use `SharedPreferences` for token persistence and a Retrofit interceptor for automatic header injection.

**Tech Stack:** Node.js, Express, PostgreSQL, Java (Android), Retrofit 2, OkHttp 3.

---

### Task 1: Backend - Multi-identifier Login and 24h Expiry

**Files:**
- Modify: `tebusco-api/src/controllers/authController.js`

- [ ] **Step 1: Update login query to include username**
Modify the SQL query in `login` function to search by `username`, `email`, or `telefono`.

```javascript
// tebusco-api/src/controllers/authController.js

// Change:
// WHERE (email = $1 OR telefono = $1)
// To:
// WHERE (email = $1 OR telefono = $1 OR username = $1)
```

- [ ] **Step 2: Update token expiration to 24 hours**
Change the `expira` calculation in both `registro` and `login` functions.

```javascript
// tebusco-api/src/controllers/authController.js

// Change:
// const expira = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000) // 7 días
// To:
// const expira = new Date(Date.now() + 24 * 60 * 60 * 1000) // 24 horas
```

- [ ] **Step 3: Verify backend changes**
Restart the API and test login with a username using `curl` or a test script.

- [ ] **Step 4: Commit**
```bash
git add tebusco-api/src/controllers/authController.js
git commit -m "feat(auth): support username login and set 24h token expiry"
```

---

### Task 2: Frontend - Session Management Utility

**Files:**
- Create: `app/src/main/java/com/codram/terecojo/utils/SessionManager.java`

- [x] **Step 1: Create SessionManager class**
Implement a singleton class to handle `SharedPreferences`.

```java
package com.codram.terecojo.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.codram.terecojo.data.model.AuthResponse;
import com.google.gson.Gson;

public class SessionManager {
    private static final String PREF_NAME = "TeRecojoPrefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER = "user_data";
    
    private static SessionManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;

    private SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context.getApplicationContext());
        }
        return instance;
    }

    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void saveUser(AuthResponse.User user) {
        String json = gson.toJson(user);
        prefs.edit().putString(KEY_USER, json).apply();
    }

    public AuthResponse.User getUser() {
        String json = prefs.getString(KEY_USER, null);
        if (json == null) return null;
        return gson.fromJson(json, AuthResponse.User.class);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
```

- [x] **Step 2: Commit**
```bash
git add app/src/main/java/com/codram/terecojo/utils/SessionManager.java
git commit -m "feat(android): add SessionManager for token persistence"
```

---

### Task 3: Frontend - API Interceptor and /me Endpoint

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/data/remote/RetrofitClient.java`
- Modify: `app/src/main/java/com/codram/terecojo/data/remote/ApiService.java`

- [ ] **Step 1: Add Auth Interceptor to RetrofitClient**
Configure `OkHttpClient` to inject the token from `SessionManager`.

```java
// app/src/main/java/com/codram/terecojo/data/remote/RetrofitClient.java

// Add OkHttpClient with Interceptor
OkHttpClient client = new OkHttpClient.Builder()
        .addInterceptor(chain -> {
            Request original = chain.request();
            String token = SessionManager.getInstance(context).getToken();
            if (token == null) return chain.proceed(original);
            
            Request request = original.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .method(original.method(), original.body())
                    .build();
            return chain.proceed(request);
        })
        .build();
```

- [ ] **Step 2: Add getMe endpoint to ApiService**
```java
// app/src/main/java/com/codram/terecojo/data/remote/ApiService.java

@GET("api/auth/me")
Call<ApiResponse<AuthResponse.User>> getMe();
```

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/codram/terecojo/data/remote/RetrofitClient.java app/src/main/java/com/codram/terecojo/data/remote/ApiService.java
git commit -m "feat(android): add auth interceptor and /me endpoint"
```

---

### Task 4: Frontend - Login Logic with Remember Me and Auto-login

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/LoginActivity.java`

- [x] **Step 1: Implement auto-login check in onCreate**
Check if a token exists and validate it via `/api/auth/me`.

- [x] **Step 2: Update onLoginClicked to handle "Remember Me"**
Save the token and user data if the checkbox is checked.

- [x] **Step 3: Implement navigateToMain(User user) helper**
Centralize navigation logic.

- [x] **Step 4: Commit**
```bash
git add app/src/main/java/com/codram/terecojo/LoginActivity.java
git commit -m "feat(android): implement auto-login and remember me logic"
```
