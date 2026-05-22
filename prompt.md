Aquí tienes el prompt completo y actualizado:

---

**Contexto del proyecto:**
Tengo una app de transporte llamada "Te Busco" compuesta por una API REST (Node.js/Express + PostgreSQL) y una app Android (Java). Los choferes actualmente ven y reciben notificaciones solo de solicitudes de su municipio. Necesito ampliar eso a nivel de provincia.

**Cambio requerido:**
Modificar el sistema para que los choferes vean todas las solicitudes activas de su provincia, reciban notificaciones push de nuevas solicitudes en toda su provincia, y esas notificaciones también se persistan en la tabla `notificaciones` para que sean visibles en el panel de notificaciones de la app.

**Archivos a modificar:**

**1. `tebusco-api/src/controllers/solicitudController.js` — función `getTodasSolicitudesActivas`**

Actualmente obtiene solo el `id` del chofer desde la tabla `choferes`. Debe también obtener `provincia_base_id`. Luego agregar un filtro `WHERE origen_provincia_id = [provincia_base_id del chofer]` a la query que consulta `v_solicitudes`. Si el chofer no tiene `provincia_base_id` definido, devolver array vacío.

**2. `tebusco-api/src/controllers/solicitudController.js` — función `createSolicitud`, bloque de notificaciones**

Actualmente construye el topic como `` `municipio_${target_municipio_id}` `` y usa `resolved_origen_municipio_id` como target. Cambiar para que use `resolved_origen_provincia_id` y el topic sea `` `provincia_${resolved_origen_provincia_id}` ``. Si `resolved_origen_provincia_id` es null, no enviar notificación.

**3. `tebusco-api/src/services/notificationService.js` — bloque de persistencia en base de datos para topics**

Actualmente el bloque de persistencia tiene esta condición:
```javascript
if (topic && topic.startsWith('municipio_')) {
```
Que extrae el ID así:
```javascript
const municipioId = topic.replace('municipio_', '');
```
Y consulta choferes así:
```javascript
SELECT usuario_id FROM choferes WHERE municipio_base_id = $1
```

Debe cambiarse para manejar el nuevo topic de provincia. La condición pasa a ser `topic.startsWith('provincia_')`, el ID se extrae con `topic.replace('provincia_', '')`, y la query de choferes cambia a `WHERE provincia_base_id = $1`. No tocar el bloque `else if (usuario_id)` que maneja persistencia individual.

**4. `app/src/main/java/com/codram/terecojo/LoginActivity.java` — bloque de suscripción FCM post-login (línea ~158)**

Actualmente suscribe al chofer a `` `municipio_` + auth.getUser().getMunicipio_id() ``. Cambiar a `` `provincia_` + auth.getUser().getProvincia_id() ``. El campo `getProvincia_id()` ya existe en el modelo `AuthResponse.User`.

**5. `app/src/main/java/com/codram/terecojo/utils/SessionManager.java` — método `logout`, bloque de desuscripción FCM (línea ~63)**

Actualmente desuscribe del topic `` `municipio_` + user.getMunicipio_id() ``. Cambiar a `` `provincia_` + user.getProvincia_id() ``.

**6. `app/src/main/java/com/codram/terecojo/DriverActivity.java` — método `onCreate`, después de `viewModel.fetchMyVehicles()`**

Agregar lógica de suscripción al topic de provincia al arrancar la actividad. Obtener el usuario desde `SessionManager.getInstance(this).getUser()`. Si el usuario no es null, su tipo es `"chofer"` y `getProvincia_id()` no es null, suscribir a `` `provincia_` + user.getProvincia_id() `` usando `FirebaseMessaging.getInstance().subscribeToTopic(topic)`. Agregar log con tag `"FCM"` indicando el topic al que se suscribió. Esto actúa como mecanismo de recuperación para choferes que ya tienen la app instalada y no han vuelto a hacer login desde el cambio.

**Restricciones:**
- No cambiar ningún otro comportamiento existente.
- No modificar la estructura de la tabla ni el modelo de datos.
- Mantener el estilo de código existente en cada archivo.
- Los logs en Android deben usar el tag `"FCM"` igual que los existentes.
- En la API, si algún ID necesario es null, fallar silenciosamente — no lanzar error, simplemente no enviar notificación o devolver array vacío según el caso.