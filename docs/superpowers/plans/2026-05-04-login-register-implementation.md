# Login and Registration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Login and Registration activities with Material 3, Java, and ViewBinding, including navigation between them.

**Architecture:** Two Activities (`LoginActivity`, `RegisterActivity`) using ViewBinding for UI interaction. Layouts defined in XML using Material Design 3 components.

**Tech Stack:** Java 11, Material Design 3, ViewBinding, Android SDK 36.

---

### Task 1: Project Configuration

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Enable ViewBinding**
Add `viewBinding = true` inside the `android` block.

```kotlin
android {
    ...
    buildFeatures {
        viewBinding = true
    }
}
```

- [ ] **Step 2: Sync and Commit**
Run `./gradlew build` to verify configuration.
`git add app/build.gradle.kts && git commit -m "config: enable viewBinding"`

---

### Task 2: Assets and Resources

**Files:**
- Create: `app/src/main/res/drawable/ic_logo.png` (copy from `icono.png`)
- Create: `app/src/main/res/layout/` (directory)
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Copy app logo**
Copy `D:\Rolo\TeRecojo\icono.png` to `app/src/main/res/drawable/ic_logo.png`.

- [ ] **Step 2: Add String resources**
Add necessary strings for hints, buttons, and labels.

```xml
<resources>
    <string name="app_name">TeRecojo</string>
    <string name="login_title">Iniciar Sesión</string>
    <string name="username_hint">Usuario</string>
    <string name="password_hint">Contraseña</string>
    <string name="remember_me">Mantener logueado</string>
    <string name="enter_button">Entrar</string>
    <string name="not_registered">¿No tienes cuenta? Regístrate</string>
    
    <string name="register_title">Registro</string>
    <string name="full_name_hint">Nombre Completo</string>
    <string name="phone_hint">Teléfono</string>
    <string name="province_hint">Provincia</string>
    <string name="municipality_hint">Municipio</string>
    <string name="passenger">Pasajero</string>
    <string name="driver">Conductor</string>
    <string name="register_button">Registrar</string>
</resources>
```

- [ ] **Step 3: Commit resources**
`git add app/src/main/res/ && git commit -m "res: add logo and strings"`

---

### Task 3: Login Layout and Activity

**Files:**
- Create: `app/src/main/res/layout/activity_login.xml`
- Create: `app/src/main/java/com/codram/terecojo/LoginActivity.java`

- [ ] **Step 1: Create Login Layout**
Implement a layout with Logo, Username, Password, Checkbox, Button, and Register Link.

- [ ] **Step 2: Implement LoginActivity**
Set up ViewBinding and click listener for the register link.

```java
package com.codram.terecojo;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.codram.terecojo.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tvRegisterLink.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        });
    }
}
```

- [ ] **Step 3: Commit Login**
`git add app/src/main/res/layout/activity_login.xml app/src/main/java/com/codram/terecojo/LoginActivity.java && git commit -m "feat: implement LoginActivity"`

---

### Task 4: Registration Layout and Activity

**Files:**
- Create: `app/src/main/res/layout/activity_register.xml`
- Create: `app/src/main/java/com/codram/terecojo/RegisterActivity.java`

- [ ] **Step 1: Create Registration Layout**
Implement form with `NestedScrollView`, `TextInputLayouts`, `Spinners`, and `MaterialButtonToggleGroup`.

- [ ] **Step 2: Implement RegisterActivity**
Set up ViewBinding and logic for the type selector toggle.

- [ ] **Step 3: Commit Registration**
`git add app/src/main/res/layout/activity_register.xml app/src/main/java/com/codram/terecojo/RegisterActivity.java && git commit -m "feat: implement RegisterActivity"`

---

### Task 5: Manifest and Final Setup

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Register Activities**
Add both activities to the manifest and set `LoginActivity` as the main entry point.

- [ ] **Step 2: Verify Build**
Run `./gradlew assembleDebug` to ensure everything compiles correctly.

- [ ] **Step 3: Commit Manifest**
`git add app/src/main/AndroidManifest.xml && git commit -m "config: update manifest with new activities"`
