# Design Doc: Admin Endpoints - Te Busco API

## Overview
Implementation of a new administration module for Te Busco API, including statistics, driver management, user management, request monitoring, and broadcast notifications.

## Architecture
- **Controller:** `src/controllers/adminController.js` will contain all the logic for the admin endpoints.
- **Routes:** `src/routes/admin.js` will define the routes and apply `authenticate` and `authorize('admin')` middleware.
- **Integration:** The new router will be registered in `src/index.js` under `/api/admin`.

## Endpoints

### 1. Stats
- `GET /api/admin/stats`: Aggregate metrics using `Promise.all`.

### 2. Driver Management
- `GET /api/admin/choferes`: Paginated list of drivers with filtering.
- `GET /api/admin/choferes/:id`: Detailed view of a driver, including vehicles, ratings, and recent requests.
- `POST /api/admin/choferes/:id/aprobar`: Approve a driver (transactional), update status to 'disponible', and send push notification.
- `POST /api/admin/choferes/:id/rechazar`: Reject a driver (transactional), reset status, and send push notification with optional reason.

### 3. User Management
- `GET /api/admin/usuarios`: Paginated list of users (excluding admins) with filtering and search.
- `PATCH /api/admin/usuarios/:id/toggle-activo`: Enable/disable user accounts and send push notification.

### 4. Request Monitoring
- `GET /api/admin/solicitudes`: Paginated list of ride requests with passenger and driver info.

### 5. Broadcast Notifications
- `POST /api/admin/notificaciones/broadcast`: Send push notifications to all users or a specific type (passenger/driver) using `Promise.allSettled`.

## Implementation Details
- **Database:** Use `pg` client with the project's transaction pattern where necessary.
- **Notifications:** Use `sendNotification` service outside of transactions.
- **Response Format:** Use `src/utils/response.js` helpers.
- **ES Modules:** Use `import`/`export` syntax.

## Testing Strategy
- Manual testing of each endpoint using a REST client (e.g., Postman/Insomnia) or curl.
- Verify push notifications are sent (check logs).
- Verify database state changes after approvals/rejections/toggles.
