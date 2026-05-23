# Design Specification: Province and Municipality Name Normalization

## Context
The passenger application sends province and municipality names as returned by Google Geocoding. These names often vary in language (e.g., "Havana" vs "La Habana") or lack proper accentuation (e.g., "Holguin" vs "Holguín"), causing the backend's `resolverUbicacion` to fail to find matches in the database.

## Goal
Standardize province and municipality names before they are used to resolve database IDs in the `createSolicitud` controller.

## Proposed Changes

### 1. `normalizarNombre` Function
A local helper function `normalizarNombre` will be defined inside the `createSolicitud` function in `tebusco-api/src/controllers/solicitudController.js`.

**Function logic:**
- **Input:** String `nombre`.
- **Process:**
  - If `nombre` is null/undefined, return it.
  - Trim and lowercase the input.
  - Check against a pre-defined mapping of common variations to their canonical Spanish names stored in the database.
- **Output:** Canonical string or the original trimmed string if no match is found.

**Mapping:**
- `havana` -> `La Habana`
- `holguin`, `holguín` -> `Holguín`
- `camaguey`, `camagüey` -> `Camagüey`
- `guantanamo`, `guantánamo` -> `Guantánamo`
- `sancti spiritus`, `sancti spíritus` -> `Sancti Spíritus`
- `ciego de avila`, `ciego de ávila` -> `Ciego de Ávila`
- `pinar del rio`, `pinar del río` -> `Pinar del Río`
- `isla de la juventud`, `isle of youth` -> `Isla de la Juventud`

### 2. Implementation in `createSolicitud`
The `resolverUbicacion` calls for both origin and destination will be updated to wrap the input names with `normalizarNombre`.

```javascript
// Example for Origin
const { pId, mId } = await resolverUbicacion(
  normalizarNombre(origen_provincia_nombre),
  normalizarNombre(origen_municipio_nombre)
);
```

## Constraints
- **Scope:** Only modify `tebusco-api/src/controllers/solicitudController.js`.
- **Placement:** `normalizarNombre` must be defined *inside* `createSolicitud`.
- **Regression:** No changes to `resolverUbicacion` logic itself.

## Verification
- Manual verification through API calls (simulated by the user) or log analysis if available.
- No automated tests will be added as per user request.
