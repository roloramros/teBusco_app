# Location Name Normalization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Standardize province and municipality names in `createSolicitud` to ensure they match database records, even when provided in English or without accents by Google Geocoding.

**Architecture:** Define a local helper function `normalizarNombre` within `createSolicitud` and apply it to location input fields before database resolution.

**Tech Stack:** Node.js, Express, PostgreSQL.

---

### Task 1: Add `normalizarNombre` helper to `createSolicitud`

**Files:**
- Modify: `tebusco-api/src/controllers/solicitudController.js`

- [ ] **Step 1: Locate `createSolicitud` and insert `normalizarNombre`**

Insert the function right before the `resolverUbicacion` definition (around line 45).

```javascript
    // Helper para normalizar nombres de provincias/municipios
    const normalizarNombre = (nombre) => {
      if (!nombre) return nombre;
      const mapa = {
        'havana': 'La Habana',
        'holguin': 'Holguín',
        'holguín': 'Holguín',
        'camaguey': 'Camagüey',
        'camagüey': 'Camagüey',
        'guantanamo': 'Guantánamo',
        'guantánamo': 'Guantánamo',
        'sancti spiritus': 'Sancti Spíritus',
        'sancti spíritus': 'Sancti Spíritus',
        'ciego de avila': 'Ciego de Ávila',
        'ciego de ávila': 'Ciego de Ávila',
        'pinar del rio': 'Pinar del Río',
        'pinar del río': 'Pinar del Río',
        'isla de la juventud': 'Isla de la Juventud',
        'isle of youth': 'Isla de la Juventud',
      };
      return mapa[nombre.toLowerCase().trim()] || nombre;
    };
```

- [ ] **Step 2: Commit changes**

```bash
git add tebusco-api/src/controllers/solicitudController.js
git commit -m "feat(api): add normalizarNombre helper to createSolicitud"
```

### Task 2: Apply normalization to origin and destination resolution

**Files:**
- Modify: `tebusco-api/src/controllers/solicitudController.js`

- [ ] **Step 1: Update origin resolution**

Update the call to `resolverUbicacion` for the origin (around line 72).

```javascript
    if (origen_municipio_nombre) {
      const { pId, mId } = await resolverUbicacion(
        normalizarNombre(origen_provincia_nombre),
        normalizarNombre(origen_municipio_nombre)
      );
      resolved_origen_provincia_id = pId;
      resolved_origen_municipio_id = mId || resolved_origen_municipio_id;
    }
```

- [ ] **Step 2: Update destination resolution**

Update the call to `resolverUbicacion` for the destination (around line 78).

```javascript
    if (destino_municipio_nombre) {
      const { pId, mId } = await resolverUbicacion(
        normalizarNombre(destino_provincia_nombre),
        normalizarNombre(destino_municipio_nombre)
      );
      resolved_destino_provincia_id = pId;
      resolved_destino_municipio_id = mId;
    }
```

- [ ] **Step 3: Verify syntax**

Run a quick syntax check using node.
Run: `node --check tebusco-api/src/controllers/solicitudController.js`
Expected: No output (meaning no syntax errors).

- [ ] **Step 4: Commit changes**

```bash
git add tebusco-api/src/controllers/solicitudController.js
git commit -m "feat(api): apply normalization to origin and destination in createSolicitud"
```
