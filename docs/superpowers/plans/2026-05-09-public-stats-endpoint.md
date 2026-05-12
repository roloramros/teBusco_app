# Public Statistics Endpoint Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a GET /api/geo/stats endpoint that returns counts of users, drivers, and rides.

**Architecture:** A new controller `statsController.js` will handle the logic, querying the database for counts. The route will be registered in `geo.js` since it's a public geographic/general information endpoint.

**Tech Stack:** Node.js, Express, PostgreSQL (pg), Vitest (for testing), Supertest (for API testing).

---

### Task 1: Setup Testing Environment

**Files:**
- Modify: `tebusco-api/package.json`

- [ ] **Step 1: Install Vitest and Supertest**
Run: `npm install --save-dev vitest supertest` (in `tebusco-api` directory)

- [ ] **Step 2: Update package.json scripts**
Add `"test": "vitest"` to the scripts section.

---

### Task 2: Implement Public Stats Endpoint with TDD

**Files:**
- Create: `tebusco-api/src/controllers/statsController.js`
- Create: `tebusco-api/test/stats.test.js`
- Modify: `tebusco-api/src/routes/geo.js`

- [ ] **Step 1: Write the failing test for the stats endpoint**

Create `tebusco-api/test/stats.test.js`:
```javascript
import { describe, it, expect, vi } from 'vitest'
import request from 'supertest'
import app from '../src/index.js'
import * as db from '../src/config/database.js'

vi.mock('../src/config/database.js', () => ({
  query: vi.fn()
}))

describe('GET /api/geo/stats', () => {
  it('should return public statistics', async () => {
    db.query.mockImplementation((sql) => {
      if (sql.includes('usuarios')) return Promise.resolve({ rows: [{ count: 10 }] })
      if (sql.includes('choferes')) return Promise.resolve({ rows: [{ count: 5 }] })
      if (sql.includes('completada')) return Promise.resolve({ rows: [{ count: 20 }] })
      if (sql.includes('activa')) return Promise.resolve({ rows: [{ count: 3 }] })
      return Promise.resolve({ rows: [] })
    })

    const response = await request(app).get('/api/geo/stats')
    
    expect(response.status).toBe(200)
    expect(response.body.ok).toBe(true)
    expect(response.body.data).toEqual({
      total_usuarios: 10,
      total_choferes: 5,
      viajes_completados: 20,
      viajes_activos: 3
    })
  })
})
```

- [ ] **Step 2: Run test to verify it fails**
Run: `npm test test/stats.test.js` (in `tebusco-api` directory)
Expected: FAIL (404 Not Found or route not defined)

- [ ] **Step 3: Create the stats controller**

Create `tebusco-api/src/controllers/statsController.js`:
```javascript
import { query } from '../config/database.js'
import { success } from '../utils/response.js'

export const getPublicStats = async (req, res, next) => {
  try {
    const queries = {
      total_usuarios: "SELECT COUNT(*)::int FROM usuarios WHERE tipo != 'admin'",
      total_choferes: "SELECT COUNT(*)::int FROM choferes",
      viajes_completados: "SELECT COUNT(*)::int FROM solicitudes WHERE estado = 'completada'",
      viajes_activos: "SELECT COUNT(*)::int FROM solicitudes WHERE estado IN ('activa', 'en_proceso')"
    };

    const results = {};
    for (const [key, sql] of Object.entries(queries)) {
      const { rows } = await query(sql);
      results[key] = rows[0].count;
    }

    return success(res, results);
  } catch (err) {
    next(err);
  }
}
```

- [ ] **Step 4: Register the route**

Modify `tebusco-api/src/routes/geo.js`:
```javascript
import { Router } from 'express'
import { getProvincias, getMunicipiosByProvincia, getVehicleTypes } from '../controllers/geoController.js'
import { getPublicStats } from '../controllers/statsController.js'

const router = Router()

// ... existing routes ...

// GET /api/geo/stats
// Estadísticas públicas de la plataforma
router.get('/stats', getPublicStats)

export default router
```

- [ ] **Step 5: Run test to verify it passes**
Run: `npm test test/stats.test.js` (in `tebusco-api` directory)
Expected: PASS

- [ ] **Step 6: Commit changes**
Run: `git add . && git commit -m "backend: add public stats endpoint"`
