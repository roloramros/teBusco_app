# Province Migration Task 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Update the notification topic in `createSolicitud` from municipality-based to province-based.

**Architecture:** Modify the `createSolicitud` controller to use `resolved_origen_provincia_id` for the notification topic instead of `resolved_origen_municipio_id`.

**Tech Stack:** Node.js, Express.

---

### Task 1: Update Notification Topic in createSolicitud

**Files:**
- Modify: `tebusco-api/src/controllers/solicitudController.js`

- [ ] **Step 1: Read the current implementation of createSolicitud**

Verify the current code block around line 112 as described in the task.

- [ ] **Step 2: Apply the change to use province topic**

```javascript
<<<<
    // ─────────────────────────────────────────────────────────
    // NOTIFICACIÓN (Choferes del municipio)
    // ─────────────────────────────────────────────────────────
    if (process.env.NODE_ENV !== 'development') {
      const target_municipio_id = resolved_origen_municipio_id;
      if (target_municipio_id) {
        await sendNotification({
          usuario_id: null, // No hay un receptor único, es por tema
          tipo: 'nueva_solicitud',
          titulo: '🚕 ¡Nueva solicitud de viaje!',
          cuerpo: `${pasajeroNombre} busca viaje desde ${origen_descripcion} hasta ${destino_descripcion}`,
          datos_extra: { solicitud_id: nuevaSolicitud.id.toString() },
          topic: `municipio_${target_municipio_id}`
        });
      }
    }
====
    // ─────────────────────────────────────────────────────────
    // NOTIFICACIÓN (Choferes de la provincia)
    // ─────────────────────────────────────────────────────────
    if (process.env.NODE_ENV !== 'development') {
      const target_provincia_id = resolved_origen_provincia_id;
      if (target_provincia_id) {
        await sendNotification({
          usuario_id: null,
          tipo: 'nueva_solicitud',
          titulo: '🚕 ¡Nueva solicitud de viaje!',
          cuerpo: `${pasajeroNombre} busca viaje desde ${origen_descripcion} hasta ${destino_descripcion}`,
          datos_extra: { solicitud_id: nuevaSolicitud.id.toString() },
          topic: `provincia_${target_provincia_id}`
        });
      }
    }
>>>>
```

- [ ] **Step 3: Verify the change by reading the file**

Confirm the replacement was successful and the logic is correct.

- [ ] **Step 4: Commit the change**

```bash
git add tebusco-api/src/controllers/solicitudController.js
git commit -m "feat(api): update notification topic to province in createSolicitud"
```
