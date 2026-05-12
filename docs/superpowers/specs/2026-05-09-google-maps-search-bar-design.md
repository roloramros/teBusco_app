# Google Maps Search Bar Integration Design

**Goal:** Implementar una barra de búsqueda flotante que aparezca solo cuando el usuario está seleccionando un punto en el mapa (Origen, Destino o Parada). La barra permitirá buscar direcciones mediante Google Places Autocomplete para posicionar el mapa rápidamente, mientras se mantiene el pin central como el selector final de la ubicación exacta.

## Architecture

La arquitectura se basará en la integración del SDK de Google Places con el flujo actual de selección de mapas de `MainActivity`.

### UI Changes (`activity_main.xml`)
- Se añadirá un `MaterialCardView` flotante en la parte superior del `CoordinatorLayout`.
- El card contendrá un `TextView` que simule un campo de búsqueda (estilo Google Maps).
- Este contenedor tendrá `android:visibility="gone"` por defecto.

### Logic Changes (`MainActivity.java`)
- **Inicialización:** Se inicializará el cliente de Google Places en `onCreate` (usando la API Key existente).
- **Control de Visibilidad:**
    - Al llamar a `startMapSelection()`, se mostrará la barra de búsqueda.
    - Al llamar a `confirmLocationSelection()` o cancelar, se ocultará la barra.
- **Flujo de Autocomplete:**
    - Al hacer clic en la barra flotante, se lanzará el `Autocomplete.IntentBuilder`.
    - En `onActivityResult`, se recibirá la ubicación seleccionada y se moverá la cámara del mapa (`mMap.animateCamera`) a esa posición.

## Components

1.  **Google Places SDK:** Necesario para la funcionalidad de búsqueda de direcciones y autocompletado.
2.  **Floating Search Bar Overlay:** Layout XML con elevación y bordes redondeados.
3.  **Autocomplete Intent Handler:** Lógica para manejar la respuesta de Google Places.

## Data Flow

1.  Usuario toca "Seleccionar Origen".
2.  App entra en `SelectionMode.ORIGIN`. Pin central aparece, barra de búsqueda aparece.
3.  Usuario toca la barra de búsqueda. Se abre Google Places UI.
4.  Usuario selecciona "Calle 23, La Habana".
5.  Mapa se mueve a esa coordenada. El pin central queda sobre la calle.
6.  Usuario ajusta moviendo el mapa y presiona "Confirmar Punto".
7.  App sale de modo selección. Barra de búsqueda desaparece.

## Error Handling & Testing

- **Testing:** Verificar que la barra solo sea visible durante la selección.
- **Edge Case:** Si el usuario busca y cancela sin elegir nada en Autocomplete, el mapa debe permanecer en su posición actual.
- **Connectivity:** Manejar fallos de red durante la búsqueda de direcciones.
