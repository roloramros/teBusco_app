# Driver License System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a usage license system for drivers, including trial periods, subscription management, automatic expiration, and an admin interface.

**Architecture:** Backend Node.js/Express controllers and cron jobs for business logic and background tasks. Frontend React components for admin management.

**Tech Stack:** Node.js, Express, PostgreSQL, node-cron, React, Tailwind CSS.

---

### Task 1: Create Backend License Controller

**Files:**
- Create: `tebusco-api/src/controllers/licenciaController.js`

- [ ] **Step 1: Create `licenciaController.js` with the provided implementation**

```javascript
import { query, getClient } from '../config/database.js'
import { success, badRequest, notFound } from '../utils/response.js'
import { sendNotification } from '../services/notificationService.js'

/**
 * GET /api/admin/licencias
 * Listar todas las licencias con info del chofer.
 */
export const getLicencias = async (req, res, next) => {
  try {
    const { estado, provincia_id, alerta, page = 1, limit = 20 } = req.query
    const offset = (page - 1) * limit
    const safeLimit = Math.min(parseInt(limit), 50)

    let whereClauses = []
    let params = []

    if (estado) {
      whereClauses.push(`l.estado = $${params.length + 1}`)
      params.push(estado)
    }
    if (provincia_id) {
      whereClauses.push(`p.id = $${params.length + 1}`)
      params.push(provincia_id)
    }
    if (alerta === '1' || alerta === 'true') {
      whereClauses.push(`(
        (l.estado = 'TRIAL_ACTIVO' AND l.trial_fin BETWEEN NOW() AND NOW() + INTERVAL '7 days')
        OR
        (l.estado = 'ACTIVO' AND l.suscripcion_fin BETWEEN NOW() AND NOW() + INTERVAL '7 days')
      )`)
    }

    const whereSQL = whereClauses.length > 0 ? `WHERE ${whereClauses.join(' AND ')}` : ''

    const sql = `
      SELECT
        l.id, l.estado, l.trial_inicio, l.trial_fin,
        l.suscripcion_inicio, l.suscripcion_fin,
        l.ultimo_pago, l.monto_mensual, l.notas,
        l.creada_en, l.actualizada_en,
        c.id AS chofer_id,
        u.id AS usuario_id, u.nombre, u.username, u.telefono,
        u.verificado, u.fcm_token,
        p.nombre AS provincia,
        CASE
          WHEN l.estado = 'TRIAL_ACTIVO'
            THEN GREATEST(0, EXTRACT(DAY FROM l.trial_fin - NOW())::int)
          WHEN l.estado = 'ACTIVO'
            THEN GREATEST(0, EXTRACT(DAY FROM l.suscripcion_fin - NOW())::int)
          ELSE 0
        END AS dias_restantes
      FROM licencias_chofer l
      JOIN choferes c ON c.id = l.chofer_id
      JOIN usuarios u ON u.id = c.usuario_id
      LEFT JOIN municipios m ON m.id = c.municipio_base_id
      LEFT JOIN provincias p ON p.id = m.provincia_id
      ${whereSQL}
      ORDER BY l.actualizada_en DESC
      LIMIT $${params.length + 1} OFFSET $${params.length + 2}
    `

    const countSQL = `
      SELECT COUNT(*)
      FROM licencias_chofer l
      JOIN choferes c ON c.id = l.chofer_id
      JOIN usuarios u ON u.id = c.usuario_id
      LEFT JOIN municipios m ON m.id = c.municipio_base_id
      LEFT JOIN provincias p ON p.id = m.provincia_id
      ${whereSQL}
    `

    const [{ rows: data }, { rows: countRows }] = await Promise.all([
      query(sql, [...params, safeLimit, offset]),
      query(countSQL, params)
    ])

    return success(res, {
      data,
      total: parseInt(countRows[0].count),
      page: parseInt(page),
      limit: safeLimit
    })
  } catch (err) {
    next(err)
  }
}

/**
 * GET /api/admin/licencias/:chofer_id
 */
export const getLicenciaByChofer = async (req, res, next) => {
  try {
    const { chofer_id } = req.params

    const { rows } = await query(`
      SELECT
        l.*,
        u.nombre, u.username, u.telefono, u.verificado, u.fcm_token,
        p.nombre AS provincia,
        CASE
          WHEN l.estado = 'TRIAL_ACTIVO'
            THEN GREATEST(0, EXTRACT(DAY FROM l.trial_fin - NOW())::int)
          WHEN l.estado = 'ACTIVO'
            THEN GREATEST(0, EXTRACT(DAY FROM l.suscripcion_fin - NOW())::int)
          ELSE 0
        END AS dias_restantes
      FROM licencias_chofer l
      JOIN choferes c ON c.id = l.chofer_id
      JOIN usuarios u ON u.id = c.usuario_id
      LEFT JOIN municipios m ON m.id = c.municipio_base_id
      LEFT JOIN provincias p ON p.id = m.provincia_id
      WHERE l.chofer_id = $1
    `, [chofer_id])

    if (rows.length === 0) return notFound(res, 'Licencia no encontrada')
    return success(res, rows[0])
  } catch (err) {
    next(err)
  }
}

/**
 * POST /api/admin/licencias/:chofer_id/registrar-pago
 */
export const registrarPago = async (req, res, next) => {
  const client = await getClient()
  try {
    const { chofer_id } = req.params
    const { monto = 0, meses = 1, notas } = req.body

    if (parseInt(meses) < 1 || parseInt(meses) > 12) {
      return badRequest(res, 'El número de meses debe estar entre 1 y 12')
    }

    await client.query('BEGIN')

    const { rows: licRows } = await client.query(
      `SELECT l.*, c.usuario_id, u.fcm_token, u.nombre
       FROM licencias_chofer l
       JOIN choferes c ON c.id = l.chofer_id
       JOIN usuarios u ON u.id = c.usuario_id
       WHERE l.chofer_id = $1`,
      [chofer_id]
    )

    if (licRows.length === 0) {
      await client.query('ROLLBACK')
      return notFound(res, 'Licencia no encontrada para este chofer')
    }

    const licencia = licRows[0]

    const baseDate =
      licencia.estado === 'ACTIVO' && licencia.suscripcion_fin && new Date(licencia.suscripcion_fin) > new Date()
        ? licencia.suscripcion_fin
        : 'NOW()'

    const suscripcionFinSQL =
      baseDate === 'NOW()'
        ? `NOW() + INTERVAL '${parseInt(meses)} months'`
        : `$1::timestamptz + INTERVAL '${parseInt(meses)} months'`

    const updateParams =
      baseDate === 'NOW()'
        ? [monto, notas || null, chofer_id]
        : [licencia.suscripcion_fin, monto, notas || null, chofer_id]

    const suscripcionInicioSQL =
      licencia.estado === 'ACTIVO' ? 'l.suscripcion_inicio' : 'NOW()'

    await client.query(
      `UPDATE licencias_chofer l SET
        estado             = 'ACTIVO',
        suscripcion_inicio = ${suscripcionInicioSQL},
        suscripcion_fin    = ${suscripcionFinSQL},
        ultimo_pago        = NOW(),
        monto_mensual      = $${baseDate === 'NOW()' ? 1 : 2},
        notas              = $${baseDate === 'NOW()' ? 2 : 3},
        actualizada_en     = NOW()
       WHERE l.chofer_id   = $${baseDate === 'NOW()' ? 3 : 4}`,
      updateParams
    )

    await client.query(
      'UPDATE usuarios SET verificado = true WHERE id = $1',
      [licencia.usuario_id]
    )

    await client.query(
      `UPDATE choferes SET estado = 'disponible'
       WHERE id = $1 AND estado = 'inactivo'`,
      [chofer_id]
    )

    await client.query('COMMIT')

    sendNotification({
      usuario_id: licencia.usuario_id,
      tipo: 'sistema_alerta',
      titulo: '✅ Licencia activada',
      cuerpo: `Tu licencia de uso de Te Busco ha sido activada por ${meses} ${parseInt(meses) === 1 ? 'mes' : 'meses'}. ¡Ya puedes continuar operando!`,
      fcm_token: licencia.fcm_token
    }).catch(console.error)

    return success(res, null, `Pago registrado. Suscripción extendida por ${meses} ${parseInt(meses) === 1 ? 'mes' : 'meses'}`)
  } catch (err) {
    await client.query('ROLLBACK')
    next(err)
  } finally {
    client.release()
  }
}

/**
 * POST /api/admin/licencias/:chofer_id/cambiar-estado
 */
export const cambiarEstado = async (req, res, next) => {
  const client = await getClient()
  try {
    const { chofer_id } = req.params
    const { estado, notas } = req.body

    const estadosValidos = ['TRIAL_ACTIVO', 'TRIAL_EXPIRADO', 'ACTIVO', 'SUSPENDIDO', 'BLOQUEADO']
    if (!estadosValidos.includes(estado)) {
      return badRequest(res, `Estado inválido. Válidos: ${estadosValidos.join(', ')}`)
    }

    await client.query('BEGIN')

    const { rows: licRows } = await client.query(
      `SELECT l.*, c.usuario_id, u.fcm_token
       FROM licencias_chofer l
       JOIN choferes c ON c.id = l.chofer_id
       JOIN usuarios u ON u.id = c.usuario_id
       WHERE l.chofer_id = $1`,
      [chofer_id]
    )

    if (licRows.length === 0) {
      await client.query('ROLLBACK')
      return notFound(res, 'Licencia no encontrada')
    }

    const licencia = licRows[0]

    const extraUpdate = estado === 'TRIAL_ACTIVO'
      ? `, trial_inicio = NOW(), trial_fin = NOW() + INTERVAL '45 days'`
      : ''

    await client.query(
      `UPDATE licencias_chofer SET
         estado = $1,
         notas = COALESCE($2, notas),
         actualizada_en = NOW()
         ${extraUpdate}
       WHERE chofer_id = $3`,
      [estado, notas || null, chofer_id]
    )

    const puedeOperar = ['TRIAL_ACTIVO', 'ACTIVO'].includes(estado)
    await client.query(
      'UPDATE usuarios SET verificado = $1 WHERE id = $2',
      [puedeOperar, licencia.usuario_id]
    )

    if (!puedeOperar) {
      await client.query(
        `UPDATE choferes SET estado = 'inactivo' WHERE id = $1`,
        [chofer_id]
      )
    }

    await client.query('COMMIT')

    const mensajes = {
      BLOQUEADO: {
        titulo: '🚫 Licencia bloqueada',
        cuerpo: 'Tu licencia de Te Busco ha sido bloqueada. Contacta al soporte para más información.'
      },
      SUSPENDIDO: {
        titulo: '⚠️ Licencia suspendida',
        cuerpo: 'Tu suscripción ha vencido. Renueva tu licencia para volver a operar en Te Busco.'
      },
      TRIAL_ACTIVO: {
        titulo: '🎉 Trial extendido',
        cuerpo: 'Tu período de prueba ha sido extendido por 45 días. ¡Sigue usando Te Busco!'
      },
      ACTIVO: {
        titulo: '✅ Licencia reactivada',
        cuerpo: 'Tu licencia ha sido reactivada manualmente. Ya puedes operar con normalidad.'
      }
    }

    if (mensajes[estado]) {
      sendNotification({
        usuario_id: licencia.usuario_id,
        tipo: 'sistema_alerta',
        ...mensajes[estado],
        fcm_token: licencia.fcm_token
      }).catch(console.error)
    }

    return success(res, null, `Estado de licencia actualizado a ${estado}`)
  } catch (err) {
    await client.query('ROLLBACK')
    next(err)
  } finally {
    client.release()
  }
}

/**
 * GET /api/admin/licencias/stats
 */
export const getLicenciasStats = async (req, res, next) => {
  try {
    const { rows } = await query(`
      SELECT
        COUNT(*) FILTER (WHERE estado = 'TRIAL_ACTIVO')   AS trial_activo,
        COUNT(*) FILTER (WHERE estado = 'TRIAL_EXPIRADO') AS trial_expirado,
        COUNT(*) FILTER (WHERE estado = 'ACTIVO')         AS activo,
        COUNT(*) FILTER (WHERE estado = 'SUSPENDIDO')     AS suspendido,
        COUNT(*) FILTER (WHERE estado = 'BLOQUEADO')      AS bloqueado,
        COUNT(*) FILTER (WHERE
          estado = 'TRIAL_ACTIVO'
          AND trial_fin BETWEEN NOW() AND NOW() + INTERVAL '7 days'
        ) AS trial_por_vencer,
        COUNT(*) FILTER (WHERE
          estado = 'ACTIVO'
          AND suscripcion_fin BETWEEN NOW() AND NOW() + INTERVAL '7 days'
        ) AS suscripcion_por_vencer
      FROM licencias_chofer
    `)

    return success(res, rows[0])
  } catch (err) {
    next(err)
  }
}
```

---

### Task 2: Modify Registration Logic

**Files:**
- Modify: `tebusco-api/src/controllers/authController.js`

- [ ] **Step 1: Update the registration block for drivers**

Find the block:
```javascript
    if (tipo === 'chofer') {
      await client.query(
        `INSERT INTO choferes (usuario_id, provincia_base_id, municipio_base_id)
         VALUES ($1, $2, $3)`,
        [usuario.id, provincia_id || null, municipio_id || null]
      )
    }
```
Replace with:
```javascript
    if (tipo === 'chofer') {
      const { rows: choferInserted } = await client.query(
        `INSERT INTO choferes (usuario_id, provincia_base_id, municipio_base_id)
         VALUES ($1, $2, $3) RETURNING id`,
        [usuario.id, provincia_id || null, municipio_id || null]
      )

      // ← NUEVO: Crear licencia en trial de 45 días automáticamente
      await client.query(
        `INSERT INTO licencias_chofer (chofer_id)
         VALUES ($1)`,
        [choferInserted[0].id]
      )
    }
```

---

### Task 3: Modify Driver Approval Logic

**Files:**
- Modify: `tebusco-api/src/controllers/adminController.js`

- [ ] **Step 1: Update `aprobarChofer` to create license if it doesn't exist**

Find where `UPDATE usuarios SET verificado = true` happens in `aprobarChofer` and add:
```javascript
    // ← NUEVO: Crear licencia si no existe (para choferes legacy)
    await client.query(
      `INSERT INTO licencias_chofer (chofer_id)
       VALUES ($1)
       ON CONFLICT (chofer_id) DO NOTHING`,
      [id]  // id aquí es el chofer.id, no el usuario.id
    )
```

---

### Task 4: Create Expiration Job

**Files:**
- Create: `tebusco-api/src/jobs/expireLicencias.js`

- [ ] **Step 1: Create `expireLicencias.js`**

```javascript
import cron from 'node-cron'
import { query } from '../config/database.js'
import { sendNotification } from '../services/notificationService.js'

export function startExpireLicenciasJob() {
  console.log('🪪  Job de expiración de licencias iniciado.')

  cron.schedule('0 2 * * *', async () => {
    console.log('🔄 [LicenciaJob] Revisando licencias expiradas...')
    try {
      const { rows: trialsExpirados } = await query(`
        UPDATE licencias_chofer SET
          estado = 'TRIAL_EXPIRADO',
          actualizada_en = NOW()
        WHERE estado = 'TRIAL_ACTIVO'
          AND trial_fin < NOW()
        RETURNING chofer_id
      `)

      const { rows: suscripcionesExpiradas } = await query(`
        UPDATE licencias_chofer SET
          estado = 'SUSPENDIDO',
          actualizada_en = NOW()
        WHERE estado = 'ACTIVO'
          AND suscripcion_fin < NOW()
        RETURNING chofer_id
      `)

      const choferIdsAfectados = [
        ...trialsExpirados.map(r => r.chofer_id),
        ...suscripcionesExpiradas.map(r => r.chofer_id)
      ]

      if (choferIdsAfectados.length > 0) {
        await query(`
          UPDATE usuarios u SET verificado = false
          FROM choferes c
          WHERE c.usuario_id = u.id
            AND c.id = ANY($1)
        `, [choferIdsAfectados])

        await query(`
          UPDATE choferes SET estado = 'inactivo'
          WHERE id = ANY($1)
        `, [choferIdsAfectados])

        const { rows: afectados } = await query(`
          SELECT u.id AS usuario_id, u.fcm_token, l.estado
          FROM licencias_chofer l
          JOIN choferes c ON c.id = l.chofer_id
          JOIN usuarios u ON u.id = c.usuario_id
          WHERE c.id = ANY($1)
        `, [choferIdsAfectados])

        await Promise.allSettled(afectados.map(chofer => {
          const esTrial = chofer.estado === 'TRIAL_EXPIRADO'
          return sendNotification({
            usuario_id: chofer.usuario_id,
            tipo: 'sistema_alerta',
            titulo: esTrial ? '⏰ Tu período de prueba ha terminado' : '⚠️ Tu suscripción ha vencido',
            cuerpo: esTrial
              ? 'Tu trial gratuito de 45 días ha finalizado. Contacta al administrador para activar tu licencia y seguir operando en Te Busco.'
              : 'Tu suscripción mensual ha vencido. Renueva tu licencia para volver a recibir solicitudes de viaje.',
            fcm_token: chofer.fcm_token
          })
        }))
      }

      const { rows: porVencer } = await query(`
        SELECT u.id AS usuario_id, u.fcm_token, l.estado,
               CASE
                 WHEN l.estado = 'TRIAL_ACTIVO'
                   THEN EXTRACT(DAY FROM l.trial_fin - NOW())::int
                 ELSE EXTRACT(DAY FROM l.suscripcion_fin - NOW())::int
               END AS dias_restantes
        FROM licencias_chofer l
        JOIN choferes c ON c.id = l.chofer_id
        JOIN usuarios u ON u.id = c.usuario_id
        WHERE (
          (l.estado = 'TRIAL_ACTIVO'
            AND l.trial_fin BETWEEN NOW() + INTERVAL '6 days' AND NOW() + INTERVAL '7 days')
          OR
          (l.estado = 'ACTIVO'
            AND l.suscripcion_fin BETWEEN NOW() + INTERVAL '6 days' AND NOW() + INTERVAL '7 days')
        )
      `)

      await Promise.allSettled(porVencer.map(chofer =>
        sendNotification({
          usuario_id: chofer.usuario_id,
          tipo: 'sistema_alerta',
          titulo: '📅 Tu licencia vence pronto',
          cuerpo: `Te quedan ${chofer.dias_restantes} días de ${chofer.estado === 'TRIAL_ACTIVO' ? 'período de prueba' : 'suscripción'}. Contacta al administrador para renovar y seguir operando sin interrupciones.`,
          fcm_token: chofer.fcm_token
        })
      ))

      console.log(`✅ [LicenciaJob] Trials expirados: ${trialsExpirados.length} | Suscripciones vencidas: ${suscripcionesExpiradas.length} | Alertas enviadas: ${porVencer.length}`)
    } catch (err) {
      console.error(`❌ [LicenciaJob] Error: ${err.message}`)
    }
  })
}
```

---

### Task 5: Start License Job

**Files:**
- Modify: `tebusco-api/src/index.js`

- [ ] **Step 1: Add import and start the job**

Add:
```javascript
import { startExpireLicenciasJob } from './jobs/expireLicencias.js'
```

And inside `startServer`:
```javascript
      // NUEVO
      startExpireSolicitudesJob()
      startExpireLicenciasJob()  // ← NUEVO
```

---

### Task 6: Add Admin Routes

**Files:**
- Modify: `tebusco-api/src/routes/admin.js`

- [ ] **Step 1: Add license routes**

Add import:
```javascript
import * as licenciaController from '../controllers/licenciaController.js'
```

Add routes before `export default router`:
```javascript
// Gestión de Licencias
router.get('/licencias',                              licenciaController.getLicencias)
router.get('/licencias/stats',                        licenciaController.getLicenciasStats)
router.get('/licencias/:chofer_id',                   licenciaController.getLicenciaByChofer)
router.post('/licencias/:chofer_id/registrar-pago',   licenciaController.registrarPago)
router.post('/licencias/:chofer_id/cambiar-estado',   licenciaController.cambiarEstado)
```

---

### Task 7: Update Frontend API

**Files:**
- Modify: `admin-panel/src/api/admin.js`

- [ ] **Step 1: Add license API calls**

```javascript
// Licencias
export const getLicencias = (params) =>
  api.get('/api/admin/licencias', { params }).then(res => res.data.data.data);

export const getLicenciasStats = () =>
  api.get('/api/admin/licencias/stats').then(res => res.data.data);

export const getLicenciaByChofer = (choferId) =>
  api.get(`/api/admin/licencias/${choferId}`).then(res => res.data.data);

export const registrarPago = (choferId, body) =>
  api.post(`/api/admin/licencias/${choferId}/registrar-pago`, body).then(res => res.data);

export const cambiarEstadoLicencia = (choferId, body) =>
  api.post(`/api/admin/licencias/${choferId}/cambiar-estado`, body).then(res => res.data);
```

---

### Task 8: Create Licencias Page

**Files:**
- Create: `admin-panel/src/pages/Licencias.jsx`

- [ ] **Step 1: Create `Licencias.jsx` with the provided implementation**

(Implementation provided in the prompt)

---

### Task 9: Add Route to App.jsx

**Files:**
- Modify: `admin-panel/src/App.jsx`

- [ ] **Step 1: Add route for Licencias**

Add import:
```javascript
import Licencias from './pages/Licencias';
```

Add route:
```javascript
<Route path="/licencias" element={<Licencias />} />
```

---

### Task 10: Add Sidebar Entry

**Files:**
- Modify: `admin-panel/src/components/layout/Sidebar.jsx`

- [ ] **Step 1: Add Licencias to `menuItems`**

```javascript
  { path: '/licencias',      label: 'Licencias',       icon: '🪪' },
```

---
