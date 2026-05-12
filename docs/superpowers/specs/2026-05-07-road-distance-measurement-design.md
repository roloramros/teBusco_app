# Diseño: Medición de Distancia por Carretera (Google Directions API)

Este documento detalla la implementación de una funcionalidad para calcular la distancia real por carretera de un viaje, incluyendo paradas intermedias, utilizando la Google Directions API.

## Problema
Los usuarios necesitan conocer la distancia real del viaje para proponer un precio de oferta justo. La distancia en línea recta no es suficiente en entornos urbanos o rutas con múltiples paradas.

## Objetivos
1.  Permitir al usuario calcular la distancia total de la ruta (Origen -> Paradas -> Destino) bajo demanda.
2.  Mostrar la distancia de forma clara cerca del campo de precio.
3.  Optimizar el uso de la API mediante un botón manual.

## Cambios Propuestos

### 1. Interfaz de Usuario (activity_main.xml)
- Añadir un `LinearLayout` horizontal justo encima del bloque de "Oferta Económica".
- **TextView (`tvDistance`)**: Muestra "Distancia: -- km". Estilo secundario.
- **MaterialButton (`btnCalculateDistance`)**: Botón estilo texto con el texto "CALCULAR RUTA".
- **ProgressBar (`pbDistance`)**: Un pequeño spinner circular (inicialmente `gone`) para mostrar progreso durante la carga.

### 2. Lógica de Aplicación (MainActivity.java)
- **Método `calculateRoadDistance()`**:
    - Validar que existan al menos Origen y Destino.
    - Construir la URL para la Directions API:
        - `origin`: Coordenadas del punto de origen.
        - `destination`: Coordenadas del punto de destino.
        - `waypoints`: Lista de coordenadas de las paradas intermedias (unidas por `|`).
        - `key`: La API Key de Google Maps.
    - Realizar la petición HTTP en un hilo de fondo (similar al Geocoder manual).
    - Parsear el JSON de respuesta:
        - Sumar el valor `distance.value` (en metros) de todos los objetos en el array `legs`.
    - Actualizar el UI con el total convertido a kilómetros (ej: "12.5 km").

### 3. Modelo de Datos (RideRequest.java)
- (Opcional) Añadir un campo `distancia_km` si se desea enviar este dato al servidor, aunque por ahora el requerimiento es solo mostrarlo.

## Flujo de Usuario
1.  El usuario selecciona origen, destino y opcionalmente paradas.
2.  Toca el botón "CALCULAR RUTA".
3.  El sistema consulta la API de Google.
4.  Se muestra la distancia total (ej: "Distancia: 15.4 km").
5.  El usuario ajusta su oferta basándose en esa información.

## Verificación
1.  Seleccionar una ruta simple y verificar que el cálculo coincida con Google Maps.
2.  Añadir paradas y verificar que la distancia total aumente y sea lógica.
3.  Probar sin haber seleccionado puntos para verificar manejo de errores (Toast informativo).
