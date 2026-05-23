# PROMPT — Feature: "No me interesa" para el Chofer
**Proyecto:** Te Busco App (Android Java + Node.js/Express + PostgreSQL)  
**Objetivo:** Permitir al chofer descartar una solicitud del radar para no volver a verla ni en el mapa ni en la lista, sin afectar su visibilidad para otros choferes.
 
---
 
## CONTEXTO Y LÓGICA DE NEGOCIO
 
### Cómo funciona el radar hoy
 
La query del radar en `getTodasSolicitudesActivas` ya hace un `LEFT JOIN` con `respuestas_solicitud` para saber si el chofer ya ofertó:
 
```sql
LEFT JOIN respuestas_solicitud r 
  ON r.solicitud_id = v.id 
  AND r.chofer_id = $1 
  AND r.estado != 'rechazado'   -- ← clave: si estado = 'rechazado', no lo encuentra
WHERE v.estado = 'activa' AND v.origen_provincia_id = $2
```
 
El campo `ha_respondido` en el resultado es `TRUE` cuando existe esa fila. Si el estado es `'rechazado'`, la fila no cuenta y la solicitud vuelve a aparecer como nueva.
 
### Solución elegida: estado `'descartado'`
 
Se agrega un nuevo valor de estado `'descartado'` en `respuestas_solicitud`. Al descartar:
1. Se inserta (o actualiza con `ON CONFLICT`) una fila con `estado = 'descartado'`
2. La query del radar se ajusta para ignorar también ese estado
3. La solicitud desaparece del mapa y de la lista para **ese chofer únicamente**
4. Sigue activa y visible para todos los demás choferes
**Ventajas:**
- Sin tabla nueva — reutiliza infraestructura existente
- Reversible en el futuro (se puede agregar "ver descartadas")
- El chofer no puede descartar una solicitud en la que ya ofertó (UI lo previene)
---
 
## PARTE 1 — BACKEND (Node.js/Express + PostgreSQL)
 
### Archivo: `tebusco-api/src/controllers/solicitudController.js`
 
#### Cambio 1A — Ajustar la query del radar para ignorar descartadas
 
**Ubicar** la función `getTodasSolicitudesActivas`. Encontrar esta línea:
 
```javascript
// ANTES
LEFT JOIN respuestas_solicitud r ON r.solicitud_id = v.id AND r.chofer_id = $1 AND r.estado != 'rechazado'
```
 
**Reemplazar por:**
 
```javascript
// DESPUÉS
LEFT JOIN respuestas_solicitud r ON r.solicitud_id = v.id AND r.chofer_id = $1 AND r.estado NOT IN ('rechazado', 'descartado')
```
 
El resultado es que si existe una fila con `estado = 'descartado'` para ese chofer, el JOIN no la encuentra, `ha_respondido` queda en `FALSE`, y la solicitud **no aparece** en el resultado del radar.
 
> **Importante:** También hay que filtrar las solicitudes descartadas del resultado final. Agregar al `WHERE` de la query principal:
 
```javascript
// Query completa corregida dentro de getTodasSolicitudesActivas:
let sql = `
  SELECT v.*, 
    CASE WHEN r.id IS NOT NULL THEN TRUE ELSE FALSE END as ha_respondido
  FROM v_solicitudes v
  JOIN solicitudes s ON s.id = v.id
  LEFT JOIN respuestas_solicitud r 
    ON r.solicitud_id = v.id 
    AND r.chofer_id = $1 
    AND r.estado NOT IN ('rechazado', 'descartado')
  -- Subconsulta para excluir descartadas completamente del resultado
  WHERE v.estado = 'activa' 
    AND v.origen_provincia_id = $2
    AND NOT EXISTS (
      SELECT 1 FROM respuestas_solicitud rd 
      WHERE rd.solicitud_id = v.id 
        AND rd.chofer_id = $1 
        AND rd.estado = 'descartado'
    )
`;
```
 
#### Cambio 1B — Agregar la función `descartarSolicitud`
 
Agregar esta función **nueva** al final del archivo, antes del último `export`:
 
```javascript
/**
 * El chofer descarta una solicitud (no le interesa, no vuelve a verla)
 */
export const descartarSolicitud = async (req, res, next) => {
  try {
    const { id: usuarioId } = req.usuario
    const { solicitud_id } = req.params
 
    // 1. Obtener el ID de chofer
    const { rows: choferRows } = await query(
      'SELECT id FROM choferes WHERE usuario_id = $1',
      [usuarioId]
    )
 
    if (choferRows.length === 0) {
      return badRequest(res, 'Solo los choferes pueden descartar solicitudes')
    }
    const choferId = choferRows[0].id
 
    // 2. Verificar que la solicitud existe y está activa
    const { rows: solRows } = await query(
      'SELECT id FROM solicitudes WHERE id = $1 AND estado = $2',
      [solicitud_id, 'activa']
    )
 
    if (solRows.length === 0) {
      return notFound(res, 'La solicitud no existe o ya no está activa')
    }
 
    // 3. Insertar o actualizar el registro de descarte
    //    ON CONFLICT maneja el caso en que el chofer ya tenía una fila previa
    //    (por ejemplo, si había ofertado y fue rechazado — ahora descarta)
    await query(
      `INSERT INTO respuestas_solicitud (solicitud_id, chofer_id, estado)
       VALUES ($1, $2, 'descartado')
       ON CONFLICT (solicitud_id, chofer_id) 
       DO UPDATE SET estado = 'descartado', respondido_en = NOW()`,
      [solicitud_id, choferId]
    )
 
    return success(res, null, 'Solicitud descartada correctamente')
  } catch (err) {
    next(err)
  }
}
```
 
### Archivo: `tebusco-api/src/routes/solicitud.js`
 
Agregar la nueva ruta **después** de la ruta de `responder`, antes de las rutas de ofertas:
 
```javascript
// AGREGAR esta línea:
router.post('/:solicitud_id/descartar', requireVerificado, solicitudController.descartarSolicitud)
```
 
El archivo de rutas completo con el agregado:
 
```javascript
import express from 'express'
import { authenticate, requireVerificado } from '../middleware/auth.js'
import * as solicitudController from '../controllers/solicitudController.js'
 
const router = express.Router()
 
router.use(authenticate)
 
router.get('/radar', solicitudController.getTodasSolicitudesActivas)
router.get('/mis-solicitudes', solicitudController.getMisSolicitudes)
router.get('/mis-viajes', solicitudController.getMisViajesChofer)
router.post('/mis-viajes/:id/cancelar', requireVerificado, solicitudController.cancelarViajeChofer)
router.get('/:id', solicitudController.getSolicitudById)
router.post('/', solicitudController.createSolicitud)
 
router.post('/:solicitud_id/responder', requireVerificado, solicitudController.responderSolicitud)
router.post('/:solicitud_id/descartar', requireVerificado, solicitudController.descartarSolicitud)  // ← NUEVO
 
router.get('/:solicitud_id/ofertas', solicitudController.getOfertasBySolicitud)
router.post('/ofertas/:respuesta_id/aceptar', solicitudController.aceptarRespuesta)
router.post('/ofertas/:respuesta_id/rechazar', solicitudController.rechazarRespuesta)
 
router.post('/:id/cancelar', solicitudController.cancelarSolicitud)
router.post('/:id/finalizar', solicitudController.finalizarViaje)
 
export default router
```
 
---
 
## PARTE 2 — ANDROID
 
### Archivo: `app/src/main/java/com/codram/terecojo/data/remote/ApiService.java`
 
Agregar la nueva llamada Retrofit junto a las demás de solicitudes (después de `responderSolicitud`):
 
```java
// AGREGAR esta declaración:
@POST("api/solicitudes/{id}/descartar")
Call<ApiResponse<Void>> descartarSolicitud(@Path("id") String solicitudId);
```
 
---
 
### Archivo: `app/src/main/java/com/codram/terecojo/ui/viewmodel/DriverViewModel.java`
 
Agregar el método `descartarSolicitud` al ViewModel. Agregar primero el LiveData:
 
```java
// Agregar junto a los otros MutableLiveData existentes:
private final MutableLiveData<String> descartarSuccess = new MutableLiveData<>();
 
// Agregar el getter:
public LiveData<String> getDescartarSuccess() { return descartarSuccess; }
```
 
Agregar el método al final de la clase, antes del cierre `}`:
 
```java
public void descartarSolicitud(String solicitudId) {
    RetrofitClient.getService().descartarSolicitud(solicitudId).enqueue(new Callback<ApiResponse<Void>>() {
        @Override
        public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
            if (response.isSuccessful()) {
                // Pasar el ID para que la Activity pueda eliminar el marcador/item
                descartarSuccess.postValue(solicitudId);
            } else {
                errorMessage.postValue("Error al descartar la solicitud");
            }
        }
 
        @Override
        public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
            errorMessage.postValue("Error de red: " + t.getMessage());
        }
    });
}
```
 
---
 
### Archivo: `app/src/main/java/com/codram/terecojo/DriverActivity.java`
 
#### Cambio 2A — Agregar `onDiscard` a la interfaz del listener
 
Localizar la interfaz `OnRideActionListener` en `RideRequestAdapter.java` y agregar el nuevo método. También aquí en `DriverActivity` donde se implementa la interfaz.
 
En `DriverActivity`, agregar el método `onDiscard`:
 
```java
// Nuevo método — implementa la acción de descartar desde la lista
public void onDiscard(RideRequest request) {
    new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
        .setTitle("Descartar solicitud")
        .setMessage("No volverás a ver esta solicitud en el radar. ¿Confirmas?")
        .setPositiveButton("DESCARTAR", (dialog, which) -> {
            viewModel.descartarSolicitud(request.getId());
        })
        .setNegativeButton("CANCELAR", null)
        .show();
}
```
 
#### Cambio 2B — Observar el resultado del descarte y actualizar el mapa
 
En el método `setupObservers()` de `DriverActivity`, agregar el observer para `descartarSuccess`:
 
```java
// Agregar dentro de setupObservers():
viewModel.getDescartarSuccess().observe(this, solicitudId -> {
    if (solicitudId != null) {
        Toast.makeText(this, "Solicitud descartada", Toast.LENGTH_SHORT).show();
        // 1. Eliminar el marcador del mapa
        com.google.android.gms.maps.model.Marker toRemove = null;
        for (com.google.android.gms.maps.model.Marker m : radarMarkers) {
            RideRequest tag = (RideRequest) m.getTag();
            if (tag != null && tag.getId().equals(solicitudId)) {
                toRemove = m;
                break;
            }
        }
        if (toRemove != null) {
            toRemove.remove();
            radarMarkers.remove(toRemove);
        }
        // 2. Eliminar de la lista del adapter
        radarRequests.removeIf(r -> r.getId().equals(solicitudId));
        if (adapter != null) adapter.notifyDataSetChanged();
        // 3. Limpiar la ruta si se estaba viendo la ruta de esta solicitud
        clearRouteMarkersAndPolylines();
        binding.fabClearRoute.setVisibility(View.GONE);
    }
});
```
 
#### Cambio 2C — Agregar botón "No me interesa" en el dialog de detalles del mapa
 
Localizar el método `showRequestDetailsDialog(RideRequest req)` en `DriverActivity.java`. Actualmente tiene dos botones: `OFERTAR` y `CERRAR`. Agregar un tercer botón neutral para descartar.
 
**Reemplazar** el método completo:
 
```java
private void showRequestDetailsDialog(RideRequest req) {
    com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
    builder.setTitle("Detalles de la Solicitud");
 
    StringBuilder msg = new StringBuilder();
    msg.append("👤 Pasajero: ").append(req.getPasajeroNombre()).append("\n");
    msg.append("📏 Distancia: ").append(String.format("%.1f km", req.getDistancia())).append("\n");
    msg.append("👥 Pasajeros: ").append(req.getNumPasajeros()).append("\n");
 
    int stops = (req.getParadas() != null) ? req.getParadas().size() : 0;
    msg.append("📍 Paradas: ").append(stops).append("\n");
 
    if (req.getDescripcion() != null && !req.getDescripcion().isEmpty()) {
        msg.append("\n📝 Notas: ").append(req.getDescripcion()).append("\n");
    }
 
    msg.append("\n💰 Oferta Pasajero: $").append(req.getPrecioOferta());
 
    if (req.isHaRespondido()) {
        msg.append("\n\n✅ Ya has enviado una oferta para este viaje.");
    }
 
    builder.setMessage(msg.toString());
 
    // Botón principal: Ofertar
    builder.setPositiveButton("OFERTAR", (dialog, which) -> onAccept(req));
 
    // Botón neutral: No me interesa (solo si no ha ofertado ya)
    if (!req.isHaRespondido()) {
        builder.setNeutralButton("NO ME INTERESA", (dialog, which) -> onDiscard(req));
    }
 
    // Botón negativo: Cerrar
    builder.setNegativeButton("CERRAR", null);
 
    androidx.appcompat.app.AlertDialog dialog = builder.create();
    dialog.show();
 
    // Deshabilitar Ofertar si ya respondió
    if (req.isHaRespondido()) {
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    }
 
    // Colorear el botón "No me interesa" en gris para diferenciarlo visualmente
    android.widget.Button btnNeutral = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL);
    if (btnNeutral != null) {
        btnNeutral.setTextColor(
            androidx.core.content.ContextCompat.getColor(this, R.color.gray_dark)
        );
    }
}
```
 
---
 
### Archivo: `app/src/main/java/com/codram/terecojo/ui/adapter/RideRequestAdapter.java`
 
#### Cambio 3A — Agregar `onDiscard` a la interfaz
 
```java
// ANTES
public interface OnRideActionListener {
    void onAccept(RideRequest request);
    void onViewMap(RideRequest request);
}
 
// DESPUÉS
public interface OnRideActionListener {
    void onAccept(RideRequest request);
    void onViewMap(RideRequest request);
    void onDiscard(RideRequest request);  // ← NUEVO
}
```
 
#### Cambio 3B — Agregar botón "No me interesa" en el item de la lista
 
En `onBindViewHolder`, después del bloque que maneja `btnAccept`, agregar la lógica para `btnDiscard`:
 
```java
// Agregar al final de onBindViewHolder, después del handler de btnAccept:
if (holder.btnDiscard != null) {
    // Solo mostrar el botón si el chofer no ha ofertado ni descartado ya
    if (!request.isHaRespondido()) {
        holder.btnDiscard.setVisibility(View.VISIBLE);
        holder.btnDiscard.setOnClickListener(v -> {
            if (listener != null) listener.onDiscard(request);
        });
    } else {
        holder.btnDiscard.setVisibility(View.GONE);
    }
}
```
 
Agregar `btnDiscard` al `ViewHolder`:
 
```java
// ANTES
public static class ViewHolder extends RecyclerView.ViewHolder {
    TextView tvPassengerName, tvPrice, tvDateTime;
    TextView tvDistanceApprox, tvStopsDetail, tvPassengersDetail, tvCreatedDate, tvDescription, tvOfferPrice;
    View btnAccept, btnViewMap;
 
    public ViewHolder(@NonNull View itemView) {
        super(itemView);
        // ... findViewByIds existentes
        btnAccept = itemView.findViewById(R.id.btnAccept);
        btnViewMap = itemView.findViewById(R.id.btnViewMap);
    }
}
 
// DESPUÉS
public static class ViewHolder extends RecyclerView.ViewHolder {
    TextView tvPassengerName, tvPrice, tvDateTime;
    TextView tvDistanceApprox, tvStopsDetail, tvPassengersDetail, tvCreatedDate, tvDescription, tvOfferPrice;
    View btnAccept, btnViewMap, btnDiscard;  // ← btnDiscard agregado
 
    public ViewHolder(@NonNull View itemView) {
        super(itemView);
        // ... findViewByIds existentes sin cambios
        btnAccept = itemView.findViewById(R.id.btnAccept);
        btnViewMap = itemView.findViewById(R.id.btnViewMap);
        btnDiscard = itemView.findViewById(R.id.btnDiscard);  // ← NUEVO
    }
}
```
 
---
 
### Archivo: `app/src/main/res/layout/item_ride_request.xml`
 
Agregar el botón "No me interesa" debajo del `LinearLayout` que contiene los botones `VER RUTA` y `OFERTAR`. El botón va como una segunda fila de botones, con texto pequeño y color gris para que visualmente tenga menos peso que los botones principales.
 
**Localizar** el `LinearLayout` con `android:layout_marginTop="12dp"` que contiene `btnViewMap` y `btnAccept`. **Después** de ese `LinearLayout` (pero dentro del `LinearLayout` padre vertical), agregar:
 
```xml
<!-- Botón secundario: No me interesa -->
<com.google.android.material.button.MaterialButton
    android:id="@+id/btnDiscard"
    style="@style/Widget.Material3.Button.TextButton"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_gravity="center_horizontal"
    android:layout_marginTop="4dp"
    android:text="No me interesa"
    android:textColor="@color/gray_dark"
    android:textSize="12sp"
    app:cornerRadius="8dp"
    app:icon="@android:drawable/ic_menu_close_clear_cancel"
    app:iconSize="14dp"
    app:iconTint="@color/gray_dark"
    app:iconGravity="textStart" />
```
 
> El estilo `Widget.Material3.Button.TextButton` lo hace visualmente discreto (sin fondo, sin borde), apropiado para una acción secundaria y destructiva-suave.
 
---
 
### Archivo: `app/src/main/java/com/codram/terecojo/DriverProfileActivity.java`
 
`DriverProfileActivity` también implementa `OnRideActionListener` y muestra el radar en lista. Hay que agregar el método `onDiscard` para que compile.
 
Agregar el método en `DriverProfileActivity`:
 
```java
@Override
public void onDiscard(RideRequest request) {
    // Confirmar y llamar a la API
    new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
        .setTitle("Descartar solicitud")
        .setMessage("No volverás a ver esta solicitud en el radar. ¿Confirmas?")
        .setPositiveButton("DESCARTAR", (dialog, which) -> {
            viewModel.descartarSolicitud(request.getId());
        })
        .setNegativeButton("CANCELAR", null)
        .show();
}
```
 
Agregar el observer en `setupObservers()` de `DriverProfileActivity`:
 
```java
viewModel.getDescartarSuccess().observe(this, solicitudId -> {
    if (solicitudId != null) {
        Toast.makeText(this, "Solicitud descartada", Toast.LENGTH_SHORT).show();
        requests.removeIf(r -> r.getId().equals(solicitudId));
        if (adapter != null) adapter.notifyDataSetChanged();
        updateEmptyState(requests.isEmpty());
    }
});
```
 
---