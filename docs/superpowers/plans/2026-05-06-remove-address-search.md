# Eliminación de Búsqueda de Dirección por Texto - Plan de Implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminar la búsqueda de direcciones por texto (Google Places) y forzar la selección únicamente mediante el mapa.

**Architecture:** Modificación de layouts XML para deshabilitar el foco y quitar iconos de búsqueda, y limpieza de MainActivity.java para eliminar la lógica de Autocomplete.

**Tech Stack:** Android (Java), XML Layouts.

---

### Task 1: Modificación de Interfaz de Usuario (XML)

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/layout/item_stop_input.xml`

- [ ] **Step 1: Quitar iconos de búsqueda y deshabilitar foco en activity_main.xml**

Modificar `app/src/main/res/layout/activity_main.xml`:
- En `tilOrigin` y `tilDestination`, eliminar `app:endIconMode`, `app:endIconDrawable`, `app:endIconTint`, y `app:endIconContentDescription`.
- En `etOrigin` y `etDestination`, añadir `android:focusable="false"` y `android:cursorVisible="false"`.

- [ ] **Step 2: Quitar iconos de búsqueda y deshabilitar foco en item_stop_input.xml**

Modificar `app/src/main/res/layout/item_stop_input.xml`:
- En `tilStop`, eliminar `app:endIconMode`, `app:endIconDrawable`, y `app:endIconTint`.
- En `etStopName`, añadir `android:focusable="false"` y `android:cursorVisible="false"`.

- [ ] **Step 3: Commit UI changes**

```bash
git add app/src/main/res/layout/activity_main.xml app/src/main/res/layout/item_stop_input.xml
git commit -m "ui: remove search icons and disable text input for locations"
```

---

### Task 2: Limpieza de Lógica en MainActivity.java

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/MainActivity.java`

- [ ] **Step 1: Eliminar constantes y listeners de Autocomplete**

Modificar `app/src/main/java/com/codram/terecojo/MainActivity.java`:
- Eliminar `private static final int AUTOCOMPLETE_REQUEST_CODE = 1001;`.
- En `setupMapInteractions()`, eliminar las llamadas a `setEndIconOnClickListener`.

- [ ] **Step 2: Eliminar método startAutocomplete y limpiar onActivityResult**

Modificar `app/src/main/java/com/codram/terecojo/MainActivity.java`:
- Borrar el método `private void startAutocomplete(SelectionMode mode, int stopIndex)`.
- En `onActivityResult`, eliminar el bloque `if (requestCode == AUTOCOMPLETE_REQUEST_CODE)`.

- [ ] **Step 3: Limpiar lógica de paradas en addStopField**

Modificar `app/src/main/java/com/codram/terecojo/MainActivity.java`:
- En el método `addStopField()`, eliminar la línea que configura `tilStop.setEndIconOnClickListener`.

- [ ] **Step 4: Commit logic changes**

```bash
git add app/src/main/java/com/codram/terecojo/MainActivity.java
git commit -m "refactor: remove Google Places Autocomplete logic"
```

---

### Task 3: Verificación Final

- [ ] **Step 1: Compilar el proyecto**

Run: `./gradlew assembleDebug`
Expected: SUCCESS

- [ ] **Step 2: Verificar visualmente (Manual)**
- Abrir la app.
- Confirmar que los campos Origen/Destino no tienen icono de lupa.
- Confirmar que al tocar los campos NO se abre el teclado.
- Confirmar que al tocar los campos se activa el modo de selección en el mapa.
- Añadir una parada y verificar que tampoco tiene icono de búsqueda y no es editable por teclado.
