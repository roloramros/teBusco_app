# PROMPT 3 — Android: Mapa del Pasajero con Choferes Disponibles
**Proyecto:** Te Busco App (Android Java)  
**Objetivo:** Agregar un modo de exploración en `MainActivity` que muestra en el mapa los choferes disponibles cercanos. El pasajero toca un marcador y ve la info del chofer en un BottomSheet.

---

## CONTEXTO Y FLUJO

El mapa del pasajero tiene hoy dos modos implícitos:
- **Modo publicación** (actual): pin central + bottomsheet con formulario
- **Modo exploración** (nuevo): marcadores de choferes + bottomsheet de info al tocar uno

El toggle entre modos es un FAB nuevo (`fabExplorar`). Al activarlo:
1. Se ocultan los elementos del modo publicación (pin central, searchBar, bottomSheet de formulario)
2. Se consulta `GET /api/choferes/disponibles?lat=X&lng=Y`
3. Se dibujan marcadores verdes para cada chofer disponible
4. El pasajero toca un marcador → aparece un BottomSheet con info del chofer
5. Al desactivar → se limpian los marcadores y vuelve el modo publicación

---

## PARTE 1 — NUEVO MODELO: `app/src/main/java/com/codram/terecojo/data/model/ChoferDisponible.java`

```java
package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class ChoferDisponible {

    @SerializedName("chofer_id")
    private String choferId;

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("telefono")
    private String telefono;

    @SerializedName("calificacion_promedio")
    private double calificacionPromedio;

    @SerializedName("total_viajes")
    private int totalViajes;

    @SerializedName("opera_interprovincial")
    private boolean operaInterprovincial;

    @SerializedName("lat")
    private double lat;

    @SerializedName("lng")
    private double lng;

    @SerializedName("ultima_ubicacion_en")
    private String ultimaUbicacionEn;

    @SerializedName("vehiculo_id")
    private String vehiculoId;

    @SerializedName("vehiculo_tipo")
    private String vehiculoTipo;

    @SerializedName("vehiculo_marca")
    private String vehiculoMarca;

    @SerializedName("vehiculo_modelo")
    private String vehiculoModelo;

    @SerializedName("vehiculo_color")
    private String vehiculoColor;

    @SerializedName("vehiculo_placa")
    private String vehiculoPlaca;

    @SerializedName("capacidad_pasajeros")
    private Integer capacidadPasajeros;

    @SerializedName("vehiculo_foto")
    private String vehiculoFoto;

    @SerializedName("distancia_km")
    private Double distanciaKm;

    // Getters
    public String getChoferId()             { return choferId; }
    public String getNombre()               { return nombre; }
    public String getTelefono()             { return telefono; }
    public double getCalificacionPromedio() { return calificacionPromedio; }
    public int getTotalViajes()             { return totalViajes; }
    public boolean isOperaInterprovincial() { return operaInterprovincial; }
    public double getLat()                  { return lat; }
    public double getLng()                  { return lng; }
    public String getUltimaUbicacionEn()    { return ultimaUbicacionEn; }
    public String getVehiculoTipo()         { return vehiculoTipo; }
    public String getVehiculoMarca()        { return vehiculoMarca; }
    public String getVehiculoModelo()       { return vehiculoModelo; }
    public String getVehiculoColor()        { return vehiculoColor; }
    public String getVehiculoPlaca()        { return vehiculoPlaca; }
    public Integer getCapacidadPasajeros()  { return capacidadPasajeros; }
    public String getVehiculoFoto()         { return vehiculoFoto; }
    public Double getDistanciaKm()          { return distanciaKm; }
}
```

---

## PARTE 2 — MODIFICAR: `app/src/main/java/com/codram/terecojo/data/remote/ApiService.java`

Agregar la declaración del nuevo endpoint:

```java
// Agregar el import:
import com.codram.terecojo.data.model.ChoferDisponible;

// Agregar la declaración:
@GET("api/choferes/disponibles")
Call<ApiResponse<List<ChoferDisponible>>> getChoferesDisponibles(
    @Query("lat") double lat,
    @Query("lng") double lng
);
```

---

## PARTE 3 — NUEVO LAYOUT: `app/src/main/res/layout/bottom_sheet_chofer.xml`

Este BottomSheet se muestra al tocar el marcador de un chofer:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@drawable/bg_bottom_sheet"
    android:paddingBottom="24dp">

    <!-- Handle -->
    <View
        android:id="@+id/handle"
        android:layout_width="40dp"
        android:layout_height="4dp"
        android:layout_marginTop="12dp"
        android:background="@drawable/bg_bottom_sheet_handle"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Nombre del chofer -->
    <TextView
        android:id="@+id/tvChoferNombre"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginTop="24dp"
        android:layout_marginStart="20dp"
        android:layout_marginEnd="20dp"
        android:textSize="18sp"
        android:textStyle="bold"
        android:textColor="@color/text_primary"
        app:layout_constraintTop_toBottomOf="@id/handle"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Calificación y viajes -->
    <LinearLayout
        android:id="@+id/layoutCalificacion"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="6dp"
        android:layout_marginStart="20dp"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        app:layout_constraintTop_toBottomOf="@id/tvChoferNombre"
        app:layout_constraintStart_toStartOf="parent">

        <TextView
            android:id="@+id/tvEstrellas"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="★"
            android:textColor="#FFC107"
            android:textSize="16sp" />

        <TextView
            android:id="@+id/tvCalificacion"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="4dp"
            android:textSize="14sp"
            android:textColor="@color/text_secondary" />

    </LinearLayout>

    <!-- Divider -->
    <View
        android:id="@+id/divider1"
        android:layout_width="0dp"
        android:layout_height="1dp"
        android:layout_marginTop="16dp"
        android:layout_marginStart="20dp"
        android:layout_marginEnd="20dp"
        android:background="#F0F0F0"
        app:layout_constraintTop_toBottomOf="@id/layoutCalificacion"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Info del vehículo -->
    <LinearLayout
        android:id="@+id/layoutVehiculo"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:layout_marginStart="20dp"
        android:layout_marginEnd="20dp"
        android:orientation="vertical"
        app:layout_constraintTop_toBottomOf="@id/divider1"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Vehículo"
            android:textSize="11sp"
            android:textColor="@color/text_secondary"
            android:textAllCaps="true"
            android:letterSpacing="0.05" />

        <TextView
            android:id="@+id/tvVehiculoInfo"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:textSize="15sp"
            android:textColor="@color/text_primary"
            android:textStyle="bold" />

        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:orientation="horizontal"
            android:gravity="center_vertical">

            <TextView
                android:id="@+id/tvVehiculoColor"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="13sp"
                android:textColor="@color/text_secondary" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text=" · "
                android:textSize="13sp"
                android:textColor="@color/text_secondary" />

            <TextView
                android:id="@+id/tvVehiculoPlaca"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="13sp"
                android:textColor="@color/text_secondary" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text=" · "
                android:textSize="13sp"
                android:textColor="@color/text_secondary" />

            <TextView
                android:id="@+id/tvCapacidad"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="13sp"
                android:textColor="@color/text_secondary" />

        </LinearLayout>

    </LinearLayout>

    <!-- Tags: Interprovincial + Distancia -->
    <LinearLayout
        android:id="@+id/layoutTags"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:layout_marginStart="20dp"
        android:layout_marginEnd="20dp"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        app:layout_constraintTop_toBottomOf="@id/layoutVehiculo"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <TextView
            android:id="@+id/tvInterprovincial"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="🛣 Interprovincial"
            android:textSize="12sp"
            android:textColor="@color/primary_blue"
            android:background="@drawable/bg_tag_blue"
            android:paddingStart="10dp"
            android:paddingEnd="10dp"
            android:paddingTop="4dp"
            android:paddingBottom="4dp"
            android:visibility="gone" />

        <TextView
            android:id="@+id/tvDistancia"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="8dp"
            android:textSize="12sp"
            android:textColor="@color/text_secondary"
            android:background="@drawable/bg_rounded_gray"
            android:paddingStart="10dp"
            android:paddingEnd="10dp"
            android:paddingTop="4dp"
            android:paddingBottom="4dp"
            android:visibility="gone" />

    </LinearLayout>

    <!-- Botón: Publicar solicitud para este chofer -->
    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnContactarChofer"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginTop="20dp"
        android:layout_marginStart="20dp"
        android:layout_marginEnd="20dp"
        android:text="Publicar solicitud de viaje"
        app:cornerRadius="12dp"
        app:layout_constraintTop_toBottomOf="@id/layoutTags"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

---

## PARTE 4 — NUEVO DRAWABLE: `app/src/main/res/drawable/bg_tag_blue.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#E3F0FF" />
    <corners android:radius="20dp" />
</shape>
```

---

## PARTE 5 — MODIFICAR: `app/src/main/java/com/codram/terecojo/MainActivity.java`

### Cambio 5A — Nuevas variables de instancia

Agregar al inicio de la clase, junto a las demás variables:

```java
// Modo exploración de choferes
private boolean modoExploracion = false;
private List<com.google.android.gms.maps.model.Marker> choferMarkers = new ArrayList<>();
private com.google.android.material.bottomsheet.BottomSheetBehavior<View> choferSheetBehavior;
private View choferBottomSheet;
```

### Cambio 5B — En `onCreate()`, después de `setupWindowInsets()`

Agregar:

```java
setupModoExploracion();
```

### Cambio 5C — Nuevo método `setupModoExploracion()`

```java
private void setupModoExploracion() {
    // Inflar el BottomSheet del chofer y agregarlo al CoordinatorLayout o ConstraintLayout raíz
    choferBottomSheet = getLayoutInflater().inflate(R.layout.bottom_sheet_chofer, null);
    choferSheetBehavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(choferBottomSheet);
    choferSheetBehavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN);
    ((android.view.ViewGroup) binding.getRoot().findViewById(R.id.main_content))
        .addView(choferBottomSheet);

    // FAB de exploración — se agrega al layout en el Paso 6
    if (binding.fabExplorar == null) return;

    binding.fabExplorar.setOnClickListener(v -> {
        if (modoExploracion) {
            desactivarModoExploracion();
        } else {
            activarModoExploracion();
        }
    });
}
```

### Cambio 5D — Nuevo método `activarModoExploracion()`

```java
private void activarModoExploracion() {
    modoExploracion = true;

    // Actualizar FAB
    binding.fabExplorar.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
    binding.fabExplorar.setContentDescription("Salir del modo exploración");

    // Ocultar elementos del modo publicación
    binding.bottomSheet.setVisibility(View.GONE);
    binding.ivCentralPin.setVisibility(View.GONE);
    binding.searchBarContainer.setVisibility(View.GONE);
    binding.fabConfirmLocation.setVisibility(View.GONE);

    // Limpiar marcadores de ruta del pasajero
    if (originMarker != null) originMarker.setVisible(false);
    if (destinationMarker != null) destinationMarker.setVisible(false);
    for (com.google.android.gms.maps.model.Marker m : stopMarkers) {
        if (m != null) m.setVisible(false);
    }

    // Cargar choferes
    cargarChoferesDisponibles();
}
```

### Cambio 5E — Nuevo método `desactivarModoExploracion()`

```java
private void desactivarModoExploracion() {
    modoExploracion = false;

    // Actualizar FAB
    binding.fabExplorar.setImageResource(android.R.drawable.ic_menu_compass);
    binding.fabExplorar.setContentDescription("Ver choferes disponibles cerca");

    // Limpiar marcadores de choferes
    for (com.google.android.gms.maps.model.Marker m : choferMarkers) m.remove();
    choferMarkers.clear();

    // Ocultar BottomSheet del chofer
    if (choferSheetBehavior != null) {
        choferSheetBehavior.setState(
            com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN);
    }

    // Restaurar elementos del modo publicación
    binding.bottomSheet.setVisibility(View.VISIBLE);
    if (originMarker != null) originMarker.setVisible(true);
    if (destinationMarker != null) destinationMarker.setVisible(true);
    for (com.google.android.gms.maps.model.Marker m : stopMarkers) {
        if (m != null) m.setVisible(true);
    }
}
```

### Cambio 5F — Nuevo método `cargarChoferesDisponibles()`

```java
private void cargarChoferesDisponibles() {
    if (mMap == null) return;

    // Usar la posición actual del centro del mapa como referencia
    com.google.android.gms.maps.model.LatLng center = mMap.getCameraPosition().target;

    RetrofitClient.getService()
        .getChoferesDisponibles(center.latitude, center.longitude)
        .enqueue(new retrofit2.Callback<ApiResponse<List<ChoferDisponible>>>() {
            @Override
            public void onResponse(
                retrofit2.Call<ApiResponse<List<ChoferDisponible>>> call,
                retrofit2.Response<ApiResponse<List<ChoferDisponible>>> response
            ) {
                if (!modoExploracion) return; // El usuario salió antes de que llegara la respuesta

                // Limpiar marcadores anteriores
                for (com.google.android.gms.maps.model.Marker m : choferMarkers) m.remove();
                choferMarkers.clear();

                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null) {
                    List<ChoferDisponible> choferes = response.body().getData();

                    if (choferes.isEmpty()) {
                        Toast.makeText(MainActivity.this,
                            "No hay choferes disponibles en este momento cerca de ti.",
                            Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (ChoferDisponible chofer : choferes) {
                        com.google.android.gms.maps.model.LatLng pos =
                            new com.google.android.gms.maps.model.LatLng(
                                chofer.getLat(), chofer.getLng());

                        com.google.android.gms.maps.model.Marker marker = mMap.addMarker(
                            new com.google.android.gms.maps.model.MarkerOptions()
                                .position(pos)
                                .title(chofer.getNombre())
                                .snippet(chofer.getVehiculoMarca() != null
                                    ? chofer.getVehiculoMarca() + " · " + chofer.getVehiculoPlaca()
                                    : "Chofer disponible")
                                .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory
                                    .defaultMarker(
                                        com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN))
                        );

                        if (marker != null) {
                            marker.setTag(chofer);
                            choferMarkers.add(marker);
                        }
                    }

                    Toast.makeText(MainActivity.this,
                        choferes.size() + " chofer" + (choferes.size() == 1 ? "" : "es")
                            + " disponible" + (choferes.size() == 1 ? "" : "s") + " cerca",
                        Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(MainActivity.this,
                        "No hay choferes disponibles en este momento.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(
                retrofit2.Call<ApiResponse<List<ChoferDisponible>>> call, Throwable t
            ) {
                if (modoExploracion) {
                    Toast.makeText(MainActivity.this,
                        "Error de red. Intenta de nuevo.", Toast.LENGTH_SHORT).show();
                }
            }
        });
}
```

### Cambio 5G — Nuevo método `mostrarInfoChofer()`

```java
private void mostrarInfoChofer(ChoferDisponible chofer) {
    if (choferBottomSheet == null || choferSheetBehavior == null) return;

    // Llenar los datos en el BottomSheet
    android.widget.TextView tvNombre = choferBottomSheet.findViewById(R.id.tvChoferNombre);
    android.widget.TextView tvCalificacion = choferBottomSheet.findViewById(R.id.tvCalificacion);
    android.widget.TextView tvVehiculoInfo = choferBottomSheet.findViewById(R.id.tvVehiculoInfo);
    android.widget.TextView tvVehiculoColor = choferBottomSheet.findViewById(R.id.tvVehiculoColor);
    android.widget.TextView tvVehiculoPlaca = choferBottomSheet.findViewById(R.id.tvVehiculoPlaca);
    android.widget.TextView tvCapacidad = choferBottomSheet.findViewById(R.id.tvCapacidad);
    android.widget.TextView tvInterprovincial = choferBottomSheet.findViewById(R.id.tvInterprovincial);
    android.widget.TextView tvDistancia = choferBottomSheet.findViewById(R.id.tvDistancia);
    com.google.android.material.button.MaterialButton btnContactar =
        choferBottomSheet.findViewById(R.id.btnContactarChofer);

    tvNombre.setText(chofer.getNombre());

    // Calificación
    if (chofer.getCalificacionPromedio() > 0) {
        tvCalificacion.setText(String.format(java.util.Locale.getDefault(),
            "%.1f · %d viajes", chofer.getCalificacionPromedio(), chofer.getTotalViajes()));
    } else {
        tvCalificacion.setText("Nuevo chofer · " + chofer.getTotalViajes() + " viajes");
    }

    // Vehículo
    if (chofer.getVehiculoMarca() != null) {
        String vehiculoInfo = chofer.getVehiculoMarca();
        if (chofer.getVehiculoModelo() != null) vehiculoInfo += " " + chofer.getVehiculoModelo();
        tvVehiculoInfo.setText(vehiculoInfo);
        tvVehiculoColor.setText(chofer.getVehiculoColor() != null ? chofer.getVehiculoColor() : "");
        tvVehiculoPlaca.setText(chofer.getVehiculoPlaca() != null ? chofer.getVehiculoPlaca() : "");
        tvCapacidad.setText(chofer.getCapacidadPasajeros() != null
            ? chofer.getCapacidadPasajeros() + " pax" : "");
    } else {
        tvVehiculoInfo.setText("Sin vehículo asignado");
    }

    // Tags
    tvInterprovincial.setVisibility(chofer.isOperaInterprovincial() ? View.VISIBLE : View.GONE);
    if (chofer.getDistanciaKm() != null) {
        tvDistancia.setVisibility(View.VISIBLE);
        tvDistancia.setText(String.format(java.util.Locale.getDefault(),
            "📍 %.1f km de ti", chofer.getDistanciaKm()));
    } else {
        tvDistancia.setVisibility(View.GONE);
    }

    // Botón: al tocar, lleva al pasajero de vuelta al modo publicación
    // con el mapa centrado en la posición del chofer como referencia de destino
    btnContactar.setOnClickListener(v -> {
        choferSheetBehavior.setState(
            com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN);
        desactivarModoExploracion();
        // Centrar el mapa en la posición del chofer para que el pasajero
        // pueda usarla como referencia al publicar su solicitud
        mMap.animateCamera(com.google.android.gms.maps.CameraUpdateFactory
            .newLatLngZoom(new com.google.android.gms.maps.model.LatLng(
                chofer.getLat(), chofer.getLng()), 15f));
        Toast.makeText(this, "Publica tu solicitud y " + chofer.getNombre()
            + " podrá verla en su radar.", Toast.LENGTH_LONG).show();
    });

    choferSheetBehavior.setState(
        com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED);
}
```

### Cambio 5H — Agregar listener de marcadores en `onMapReady()`

Dentro de `onMapReady()`, después de `super.onMapReady(googleMap)`, agregar el listener de clic en marcadores:

```java
mMap.setOnMarkerClickListener(marker -> {
    Object tag = marker.getTag();
    if (tag instanceof ChoferDisponible) {
        // Marcador de chofer disponible
        mostrarInfoChofer((ChoferDisponible) tag);
        return true;
    }
    // Comportamiento por defecto para otros marcadores
    return false;
});
```

### Cambio 5I — Agregar import al inicio de `MainActivity.java`

```java
import com.codram.terecojo.data.model.ChoferDisponible;
```

---

## PARTE 6 — MODIFICAR: `app/src/main/res/layout/activity_main.xml`

Agregar el FAB de exploración dentro del `ConstraintLayout` de `main_content`, después del FAB `fabMyLocation`:

```xml
<!-- FAB: Ver choferes disponibles -->
<com.google.android.material.floatingactionbutton.FloatingActionButton
    android:id="@+id/fabExplorar"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_margin="16dp"
    android:contentDescription="Ver choferes disponibles cerca"
    android:src="@android:drawable/ic_menu_compass"
    app:backgroundTint="@color/primary_blue"
    app:tint="@color/white"
    app:layout_anchor="@id/bottomSheet"
    app:layout_anchorGravity="end|top"
    app:layout_constraintBottom_toTopOf="@id/fabMyLocation"
    app:layout_constraintEnd_toEndOf="parent" />
```

---

## RESUMEN DE ARCHIVOS

| Archivo | Tipo |
|---|---|
| `data/model/ChoferDisponible.java` | ✅ Nuevo |
| `data/remote/ApiService.java` | ✏️ 1 endpoint nuevo |
| `res/layout/bottom_sheet_chofer.xml` | ✅ Nuevo |
| `res/drawable/bg_tag_blue.xml` | ✅ Nuevo |
| `MainActivity.java` | ✏️ 2 variables + 7 métodos + 1 import |
| `res/layout/activity_main.xml` | ✏️ 1 FAB nuevo |

---

## FLUJO COMPLETO ESPERADO

1. Pasajero abre la app → ve el mapa normal con el formulario de publicación
2. Toca el FAB azul (brújula) → el formulario se oculta
3. Aparecen marcadores verdes en el mapa → cada uno es un chofer disponible
4. Toast: "3 choferes disponibles cerca"
5. El pasajero toca un marcador verde → aparece el BottomSheet con:
   - Nombre del chofer y calificación
   - Vehículo: marca, modelo, color, placa, capacidad
   - Badge "Interprovincial" si aplica
   - Distancia desde la posición del pasajero
   - Botón "Publicar solicitud de viaje"
6. Al tocar el botón → se cierra el BottomSheet, se vuelve al modo publicación, el mapa se centra cerca del chofer
7. El pasajero publica su solicitud normalmente → el chofer la ve en su radar
8. Al tocar de nuevo el FAB (ahora muestra una X) → se limpian los marcadores y vuelve el formulario
