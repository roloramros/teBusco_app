# Diseño: Soporte Multi-moneda en Creación de Solicitudes

Este documento detalla los cambios necesarios para adaptar la creación de solicitudes al nuevo esquema de base de datos donde `moneda` es un arreglo y el precio inicial es siempre cero.

## 1. Cambios en el Backend (API)

### `tebusco-api/src/controllers/solicitudController.js`
- Modificar `createSolicitud` para:
    - Recibir `moneda` como un arreglo desde `req.body`.
    - Normalizar las monedas: convertirlas a mayúsculas y eliminar espacios.
    - Establecer un valor por defecto: `['CUP']` si el arreglo está vacío o no se proporciona.
    - Asegurar que `precio_oferta` se inserte como `0` (o el valor recibido, que será 0).
    - Usar la sintaxis de PostgreSQL para insertar arreglos: `$10::text[]`.

## 2. Cambios en Android

### `app/src/main/java/com/codram/terecojo/data/model/RideRequest.java`
- Cambiar el tipo de dato del campo `moneda`:
    ```java
    private List<String> moneda;
    ```
- Actualizar el getter y setter correspondiente.

### `app/src/main/java/com/codram/terecojo/MainActivity.java`
- En el método `executeFinalPublish`:
    - Cambiar la recolección de monedas de un `StringBuilder` (String separado por comas) a una `List<String>`.
    - Asegurar que `request.setPrecioOferta(0.0)` se asigne explícitamente.
    - Pasar la lista de monedas al objeto `request`.

### `app/src/main/java/com/codram/terecojo/ui/adapter/RideRequestAdapter.java`
- Actualizar la visualización para manejar la lista de monedas:
    - Reemplazar la lógica que muestra el precio único por una que concatene las monedas (ej: "CUP, USD").
    - Mostrar "Pendiente de oferta" o similar junto a las monedas.

## 3. Validación
- Verificar que al publicar desde el app, la base de datos registre un registro con `precio_oferta = 0.00` y `moneda = {CUP,USD}` (u otras seleccionadas).
- Confirmar que si no se selecciona ninguna moneda, el API guarde `{CUP}` por defecto.
