# Diseño: Eliminación de búsqueda de dirección por texto

Este documento detalla los cambios para restringir la selección de ubicaciones en la publicación de viajes únicamente mediante el punto central del mapa, eliminando la funcionalidad de búsqueda por texto (Google Places Autocomplete).

## Problema
El usuario desea simplificar la interfaz y evitar que se introduzcan direcciones manualmente o por búsqueda de texto, forzando a que todas las ubicaciones (Origen, Destino y Paradas) se definan visualmente en el mapa para mayor precisión.

## Objetivos
1.  Eliminar la integración de Google Places Autocomplete.
2.  Deshabilitar la entrada de texto manual en los campos de ubicación.
3.  Mantener el flujo de selección mediante el "Pin Central" del mapa.

## Cambios Propuestos

### 1. Interfaz de Usuario (Layouts)
- **activity_main.xml**:
    - Eliminar `app:endIconMode="custom"` y atributos relacionados de `tilOrigin` y `tilDestination`.
    - Configurar `etOrigin` y `etDestination` como no editables (`android:focusable="false"`, `android:cursorVisible="false"`).
- **item_stop_input.xml**:
    - Eliminar `app:endIconMode="custom"` y atributos relacionados de `tilStop`.
    - Configurar `etStopName` como no editable.

### 2. Lógica de Aplicación (MainActivity.java)
- **Eliminar dependencias de Autocomplete**: Quitar la constante `AUTOCOMPLETE_REQUEST_CODE`.
- **Limpiar inicialización**: Eliminar los listeners de los iconos finales (`setEndIconOnClickListener`).
- **Eliminar métodos de búsqueda**: Borrar `startAutocomplete(SelectionMode, int)`.
- **Simplificar onActivityResult**: Eliminar el bloque que maneja el resultado de `AUTOCOMPLETE_REQUEST_CODE`.
- **Actualizar creación de paradas**: En `addStopField`, eliminar la configuración del icono de búsqueda.

## Verificación
1.  Al tocar los campos de Origen/Destino, el panel debe contraerse y mostrar el pin central (comportamiento actual mantenido).
2.  No debe aparecer el icono de la lupa o ubicación al final de los campos.
3.  No debe ser posible escribir texto en los campos ni abrir el teclado.
4.  La dirección debe actualizarse automáticamente al mover el mapa y confirmarse al presionar "Confirmar Ubicación".
