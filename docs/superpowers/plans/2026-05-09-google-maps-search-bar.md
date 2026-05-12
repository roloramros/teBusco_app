# Google Maps Search Bar Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar una barra de búsqueda flotante tipo Google Maps que aparezca solo en modo selección de ubicación para mover el mapa mediante autocompletado de direcciones.

**Architecture:** Integración de Google Places SDK. Uso de un `MaterialCardView` flotante en `activity_main.xml`. Control de visibilidad y manejo de resultados en `MainActivity.java`.

**Tech Stack:** Android (Java), Google Places SDK, XML Layouts.

---

### Task 1: Configuración de Dependencias e Inicialización

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/codram/terecojo/MainActivity.java`

- [ ] **Step 1: Añadir Google Places al Version Catalog**

Modificar `gradle/libs.versions.toml`:
```toml
[versions]
...
googlePlaces = "3.3.0"

[libraries]
...
google-places = { group = "com.google.android.libraries.places", name = "places", version.ref = "googlePlaces" }
```

- [ ] **Step 2: Añadir dependencia al build.gradle de la app**

Modificar `app/build.gradle.kts`:
```kotlin
dependencies {
    ...
    implementation(libs.google.places)
}
```

- [ ] **Step 3: Inicializar Places en MainActivity**

Modificar `app/src/main/java/com/codram/terecojo/MainActivity.java`:
```java
import com.google.android.libraries.places.api.Places;
...
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ...
    // Inicializar Google Places
    if (!Places.isInitialized()) {
        Places.initialize(getApplicationContext(), "AIzaSyBufiSwuBW19JLsbXKDbW86pg_1wL7ifxU");
    }
}
```

- [ ] **Step 4: Commit dependencies**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/java/com/codram/terecojo/MainActivity.java
git commit -m "build: add google places dependency and initialization"
```

---

### Task 2: Implementación de la Interfaz (UI)

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`

- [ ] **Step 1: Añadir la barra de búsqueda flotante**

Modificar `app/src/main/res/layout/activity_main.xml`. Añadir el `MaterialCardView` antes del `NestedScrollView` (Bottom Sheet) para que flote sobre el mapa.

```xml
        <!-- Barra de Búsqueda Flotante -->
        <com.google.android.material.card.MaterialCardView
            android:id="@+id/searchBarContainer"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_margin="16dp"
            android:layout_marginTop="80dp"
            android:visibility="gone"
            app:cardCornerRadius="24dp"
            app:cardElevation="8dp"
            app:strokeWidth="0dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:paddingHorizontal="16dp">

                <ImageView
                    android:layout_width="24dp"
                    android:layout_height="24dp"
                    android:src="@android:drawable/ic_menu_search"
                    app:tint="@color/text_secondary" />

                <TextView
                    android:id="@+id/tvSearchBar"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="12dp"
                    android:text="Buscar dirección..."
                    android:textColor="@color/text_secondary"
                    android:textSize="16sp" />
            </LinearLayout>
        </com.google.android.material.card.MaterialCardView>
```

- [ ] **Step 2: Ajustar el margen superior de la barra (Window Insets)**

Se manejará en Java para evitar solapamiento con la barra de estado.

- [ ] **Step 3: Commit UI**

```bash
git add app/src/main/res/layout/activity_main.xml
git commit -m "ui: add floating search bar to activity_main.xml"
```

---

### Task 3: Lógica de Negocio e Integración

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/MainActivity.java`

- [ ] **Step 1: Configurar constantes y variables**

Modificar `app/src/main/java/com/codram/terecojo/MainActivity.java`:
```java
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.libraries.places.api.model.Place;
import android.content.Intent;
...
private static final int AUTOCOMPLETE_REQUEST_CODE = 1001;
```

- [ ] **Step 2: Control de visibilidad de la barra**

Modificar `startMapSelection` y `confirmLocationSelection` en `MainActivity.java`:

```java
private void startMapSelection(SelectionMode mode, int stopIndex) {
    ...
    binding.searchBarContainer.setVisibility(View.VISIBLE);
}

private void confirmLocationSelection() {
    ...
    binding.searchBarContainer.setVisibility(View.GONE);
}
```

*Nota: También añadir la visibilidad GONE en `onBackPressed` si se cancela la selección.*

- [ ] **Step 3: Lanzar Autocomplete al tocar la barra**

Añadir el listener en `onCreate` o `setupMapInteractions`:
```java
binding.tvSearchBar.setOnClickListener(v -> {
    List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS);
    Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            .setCountry("CU") // Opcional: restringir a Cuba
            .build(this);
    startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
});
```

- [ ] **Step 4: Manejar el resultado de la búsqueda**

Añadir/Modificar `onActivityResult`:
```java
@Override
protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
        if (resultCode == RESULT_OK) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            if (place.getLatLng() != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 17f));
            }
        } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
            Status status = Autocomplete.getStatusFromIntent(data);
            Log.e("Places", status.getStatusMessage());
        }
    }
}
```

- [ ] **Step 5: Ajustar Window Insets para la barra flotante**

En `setupWindowInsets()`:
```java
ViewCompat.setOnApplyWindowInsetsListener(binding.searchBarContainer, (v, insets) -> {
    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
    android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
    params.topMargin = (int) (16 * getResources().getDisplayMetrics().density) + systemBars.top;
    v.setLayoutParams(params);
    return insets;
});
```

- [ ] **Step 6: Commit Logic**

```bash
git add app/src/main/java/com/codram/terecojo/MainActivity.java
git commit -m "feat: implement google places autocomplete logic for search bar"
```

---

### Task 4: Verificación y Limpieza

- [ ] **Step 1: Compilar y probar**
Run: `./gradlew assembleDebug`
- Tocar "Seleccionar Origen".
- Verificar que aparece la barra.
- Tocar la barra, buscar una dirección.
- Verificar que el mapa se mueve.
- Confirmar punto y verificar que la barra desaparece.

- [ ] **Step 2: Commit final**
```bash
git commit -m "final: verify and clean up google maps search bar integration"
```
