# Diseño: Nueva Selección de Ubicación con Botón Flotante

Este documento detalla los cambios para mejorar el flujo de selección de ubicaciones en el mapa, utilizando un botón flotante dedicado para confirmar el punto seleccionado, permitiendo que el panel inferior se expanda automáticamente para facilitar la edición de otros campos o la adición de paradas.

## Problema
Actualmente, el botón "Publicar Solicitud" cambia su texto a "Confirmar Ubicación" cuando el usuario está interactuando con el mapa. Esto es confuso y obliga al usuario a lidiar con un panel colapsado que no permite ver claramente el progreso de la solicitud o añadir paradas de forma fluida.

## Objetivos
1.  Introducir un botón flotante (FAB) dedicado exclusivamente a confirmar la ubicación en el mapa.
2.  Mantener el botón "Publicar" con una única función: publicar el viaje.
3.  Mejorar la navegación entre el mapa y el panel de detalles (Bottom Sheet).

## Cambios Propuestos

### 1. Interfaz de Usuario (Layouts)
- **activity_main.xml**:
    - Añadir un `com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton` o `FloatingActionButton` (FAB).
    - ID: `fabConfirmLocation`.
    - Ubicación: Flotando sobre el mapa, posicionado en la parte inferior derecha, justo encima del `peekHeight` del Bottom Sheet.
    - Icono: Un checkmark (e.g., `@android:drawable/ic_menu_save` o uno personalizado).
    - Texto (si es Extended): "Confirmar Punto".
    - Visibilidad inicial: `gone`.

### 2. Lógica de Aplicación (MainActivity.java)
- **setupBottomSheet**:
    - Asegurar que `btnPublishRequest` siempre tenga el listener de `publishRideRequest`.
- **startMapSelection**:
    - Mostrar `fabConfirmLocation`.
    - Ocultar `binding.btnPublishRequest` (opcional, o simplemente no cambiar su texto/listener).
    - El Bottom Sheet permanece colapsado.
- **confirmLocationSelection**:
    - Ejecutar la lógica de geocodificación y marcadores (existente).
    - Ocultar `fabConfirmLocation`.
    - Expandir el Bottom Sheet (`STATE_EXPANDED`).
    - Restaurar el estado visual de los botones si fuera necesario.

## Flujo de Usuario Revisado
1.  El usuario toca el campo "Destino".
2.  El Bottom Sheet se colapsa.
3.  Aparece el pin central y el botón flotante **"Confirmar"**.
4.  El usuario mueve el mapa hasta el punto deseado.
5.  El usuario presiona el botón flotante **"Confirmar"**.
6.  El Bottom Sheet se expande automáticamente, mostrando la dirección obtenida y permitiendo añadir paradas o publicar.

## Verificación
1.  Al seleccionar una ubicación, el botón azul de abajo NO debe cambiar su texto.
2.  Debe aparecer un botón flotante nuevo.
3.  Al pulsar el botón flotante, el panel debe abrirse solo.
4.  La publicación del viaje solo debe ocurrir al pulsar el botón azul cuando el panel está expandido.
