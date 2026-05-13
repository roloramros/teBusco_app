# Design Doc: Driver Verification Restriction - Te Busco API

## Overview
Restrict unverified drivers from sending offers to passengers. Unverified drivers (where `usuario.verificado = false`) should still be able to see active requests (radar) and manage their profile/vehicles, but cannot participate in the bidding process until approved by an administrator.

## Proposed Changes

### 1. Middleware Usage
The existing `requireVerificado` middleware in `src/middleware/auth.js` will be used to enforce this restriction. This middleware checks if `req.usuario.verificado` is truthy and returns a `403 Forbidden` response if not.

### 2. Routes Modification
Modify `src/routes/solicitud.js` to protect the offer submission endpoint.

- **File:** `src/routes/solicitud.js`
- **Target Route:** `POST /api/solicitudes/:solicitud_id/responder`
- **Change:** Add `requireVerificado` to the middleware chain for this specific route.

### 3. Data Flow
1. Driver attempts to send an offer via `POST /api/solicitudes/:solicitud_id/responder`.
2. `authenticate` middleware verifies the JWT and attaches the user object (including `verificado` status).
3. `requireVerificado` middleware checks the status.
4. If `verificado` is `false`, a `403 Forbidden` response is sent with the message: "Tu cuenta aún no ha sido verificada por un administrador".
5. If `verificado` is `true`, the request proceeds to the `solicitudController.responderSolicitud` controller.

## Implementation Details
- No changes needed to `solicitudController.js`.
- No changes needed to `auth.js` middleware (already correctly implemented).
- Minimal and surgical update to `src/routes/solicitud.js`.

## Testing Strategy
1. **Unverified Driver:**
   - Log in with a user whose `verificado` column in `usuarios` is `false`.
   - Verify `GET /api/solicitudes/radar` still returns active requests.
   - Attempt `POST /api/solicitudes/:id/responder` and verify it returns `403 Forbidden`.
2. **Verified Driver:**
   - Log in with a user whose `verificado` column is `true`.
   - Attempt `POST /api/solicitudes/:id/responder` and verify it proceeds normally (e.g., returns success or 404/400 depending on body/ID).
