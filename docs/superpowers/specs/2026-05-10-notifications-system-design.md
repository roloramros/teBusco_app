# Sistema de Notificaciones con Historial y Badge

Este documento describe la arquitectura y los pasos para implementar el sistema de notificaciones en la App de TeRecojo, incluyendo persistencia y visualización.

## 1. Experiencia de Usuario (UX)

### Menú Lateral (Nav Drawer)
- El icono de la campana (`ivNotifications`) mostrará un **punto rojo** (badge) en la esquina superior derecha si hay al menos una notificación sin leer.
- Este punto se actualizará cada vez que se abra el menú o mediante un proceso en segundo plano ligero.

### Pantalla de Notificaciones (`NotificationsActivity`)
- Una lista (RecyclerView) ordenada cronológicamente (más recientes primero).
*   **Iconografía Visual**:
    *   `💰` (Oferta): Amarillo/Verde.
    *   `✅` (Aceptada): Verde.
    *   `❌` (Rechazada/Cancelada): Rojo.
    *   `🚕` (Nueva Solicitud): Azul.
- Al tocar una notificación:
    1. Se marca como leída en el servidor.
    2. Se navega a la pantalla correspondiente (ej: Ver ofertas si es 'nueva_oferta').
- Opción de "Marcar todas como leídas".

## 2. Arquitectura Técnica

### Modelo de Datos (`Notification.java`)
```java
public class Notification {
    private String id;
    private String tipo; // nueva_oferta, oferta_aceptada, etc.
    private String titulo;
    private String cuerpo;
    private boolean leida;
    private String creada_en;
    private Map<String, String> datos_extra; // Contiene solicitud_id, etc.
}
```

### API Endpoints
- `GET /api/notificaciones`: Lista de notificaciones.
- `PATCH /api/notificaciones/{id}/read`: Marcar una.
- `POST /api/notificaciones/read-all`: Marcar todas.

## 3. Implementación Visual del "Punto Rojo"
Se utilizará un `FrameLayout` o un `ConstraintLayout` para envolver la campana y un pequeño `View` circular rojo que se activará (`View.VISIBLE`) basándose en el conteo de no leídas.

¿Te parece bien este planteamiento para proceder con el documento de diseño final y el plan de ejecución?