# PROMPT — Sistema de Licencias de Uso para Choferes
**Proyecto:** Te Busco App  
**Alcance:** Backend (Node.js/Express) + Admin Panel (React)  
**La tabla `licencias_chofer` ya fue creada manualmente en la BD.**

---

## ESTRUCTURA DE LA TABLA (referencia, no crear)

```sql
CREATE TABLE licencias_chofer (
    id                  SERIAL PRIMARY KEY,
    chofer_id           INTEGER NOT NULL UNIQUE REFERENCES choferes(id) ON DELETE CASCADE,
    estado              VARCHAR(20) NOT NULL DEFAULT 'TRIAL_ACTIVO',
    trial_inicio        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    trial_fin           TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '45 days'),
    suscripcion_inicio  TIMESTAMPTZ,
    suscripcion_fin     TIMESTAMPTZ,
    ultimo_pago         TIMESTAMPTZ,
    monto_mensual       DECIMAL(10,2) DEFAULT 0.00,
    notas               TEXT,
    creada_en           TIMESTAMPTZ DEFAULT NOW(),
    actualizada_en      TIMESTAMPTZ DEFAULT NOW()
);
```

**Estados válidos:** `TRIAL_ACTIVO`, `TRIAL_EXPIRADO`, `ACTIVO`, `SUSPENDIDO`, `BLOQUEADO`

**Regla de negocio:** Un chofer puede operar si su licencia es `TRIAL_ACTIVO` o `ACTIVO`. En cualquier otro estado, `verificado = false` y no puede ofertar.

---

## PARTE 1 — BACKEND

### 1.1 Nuevo archivo: `tebusco-api/src/controllers/licenciaController.js`

Crear este archivo completo:

```javascript
import { query, getClient } from '../config/database.js'
import { success, badRequest, notFound } from '../utils/response.js'
import { sendNotification } from '../services/notificationService.js'

/**
 * GET /api/admin/licencias
 * Listar todas las licencias con info del chofer.
 * Soporta filtros: estado, provincia_id, alerta (vence en 7 días)
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
    // alerta=1 → licencias que vencen en los próximos 7 días
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
        -- Días restantes según el estado
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
 * Ver la licencia de un chofer específico
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
 * El admin registra un pago mensual manualmente.
 * Body: { monto, meses = 1, notas }
 *
 * Lógica:
 *   - Si el chofer estaba SUSPENDIDO o TRIAL_EXPIRADO → pasa a ACTIVO
 *   - Si ya está ACTIVO → extiende suscripcion_fin N meses más
 *   - Actualiza verificado = true en usuarios
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

    // Obtener licencia actual
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

    // Calcular nueva fecha de fin de suscripción
    // Si ya tiene suscripción activa y no expiró, extender desde ese fin
    // Si no, comenzar desde ahora
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

    // Activar verificado si estaba en false
    await client.query(
      'UPDATE usuarios SET verificado = true WHERE id = $1',
      [licencia.usuario_id]
    )

    // Actualizar estado del chofer a disponible si estaba inactivo
    await client.query(
      `UPDATE choferes SET estado = 'disponible'
       WHERE id = $1 AND estado = 'inactivo'`,
      [chofer_id]
    )

    await client.query('COMMIT')

    // Notificar al chofer
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
 * El admin cambia manualmente el estado de una licencia.
 * Body: { estado, notas }
 * Útil para: bloquear manualmente, extender trial, reactivar, etc.
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

    // Si es TRIAL_ACTIVO, extender trial_fin a 45 días desde hoy
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

    // Sincronizar verificado según el nuevo estado
    const puedeOperar = ['TRIAL_ACTIVO', 'ACTIVO'].includes(estado)
    await client.query(
      'UPDATE usuarios SET verificado = $1 WHERE id = $2',
      [puedeOperar, licencia.usuario_id]
    )

    // Sincronizar estado del chofer
    if (!puedeOperar) {
      await client.query(
        `UPDATE choferes SET estado = 'inactivo' WHERE id = $1`,
        [chofer_id]
      )
    }

    await client.query('COMMIT')

    // Notificaciones según el nuevo estado
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
 * Resumen de licencias para el dashboard
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

### 1.2 Modificar: `tebusco-api/src/controllers/authController.js`

**Cambio único:** En la función `registro`, dentro del bloque `if (tipo === 'chofer')`, agregar la creación automática de la licencia justo después del INSERT en `choferes`:

```javascript
// ANTES — bloque existente (línea ~110)
if (tipo === 'chofer') {
  await client.query(
    `INSERT INTO choferes (usuario_id, provincia_base_id, municipio_base_id)
     VALUES ($1, $2, $3)`,
    [usuario.id, provincia_id || null, municipio_id || null]
  )
}

// DESPUÉS — agregar las líneas marcadas con ← NUEVO
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

> El INSERT de licencia usa solo `chofer_id` porque todos los demás campos tienen `DEFAULT` en la BD: `estado = 'TRIAL_ACTIVO'`, `trial_inicio = NOW()`, `trial_fin = NOW() + 45 days`.

---

### 1.3 Modificar: `tebusco-api/src/controllers/adminController.js`

**Cambio único:** En `aprobarChofer`, cuando el admin aprueba un chofer, también crear su licencia si aún no existe (para choferes registrados antes de implementar este sistema):

Localizar el bloque de `aprobarChofer` donde se hace `UPDATE usuarios SET verificado = true`. Agregar después:

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

### 1.4 Nuevo archivo: `tebusco-api/src/jobs/expireLicencias.js`

```javascript
import cron from 'node-cron'
import { query } from '../config/database.js'
import { sendNotification } from '../services/notificationService.js'

export function startExpireLicenciasJob() {
  console.log('🪪  Job de expiración de licencias iniciado.')

  // Ejecutar una vez al día a las 2:00 AM
  cron.schedule('0 2 * * *', async () => {
    console.log('🔄 [LicenciaJob] Revisando licencias expiradas...')
    try {
      // 1. Expirar trials vencidos
      const { rows: trialsExpirados } = await query(`
        UPDATE licencias_chofer SET
          estado = 'TRIAL_EXPIRADO',
          actualizada_en = NOW()
        WHERE estado = 'TRIAL_ACTIVO'
          AND trial_fin < NOW()
        RETURNING chofer_id
      `)

      // 2. Suspender suscripciones vencidas
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
        // 3. Desactivar verificado y poner choferes como inactivos
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

        // 4. Notificar a cada chofer afectado
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

      // 5. Alerta preventiva: notificar 7 días antes del vencimiento
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

### 1.5 Modificar: `tebusco-api/src/index.js`

Agregar el import y el arranque del nuevo job. Localizar las líneas del job existente y agregar debajo:

```javascript
// ANTES
import { startExpireSolicitudesJob } from './jobs/expireSolicitudes.js'

// DESPUÉS — agregar esta línea
import { startExpireLicenciasJob } from './jobs/expireLicencias.js'
```

Y dentro de `app.listen(PORT, () => { ... })`, agregar después de `startExpireSolicitudesJob()`:

```javascript
startExpireSolicitudesJob()
startExpireLicenciasJob()  // ← NUEVO
```

---

### 1.6 Modificar: `tebusco-api/src/routes/admin.js`

Agregar las rutas de licencias al final del archivo, antes de `export default router`:

```javascript
// Agregar este import al inicio del archivo, con los demás imports:
import * as licenciaController from '../controllers/licenciaController.js'

// Agregar estas rutas al final, antes de export default router:

// Gestión de Licencias
router.get('/licencias',                              licenciaController.getLicencias)
router.get('/licencias/stats',                        licenciaController.getLicenciasStats)
router.get('/licencias/:chofer_id',                   licenciaController.getLicenciaByChofer)
router.post('/licencias/:chofer_id/registrar-pago',   licenciaController.registrarPago)
router.post('/licencias/:chofer_id/cambiar-estado',   licenciaController.cambiarEstado)
```

> **Orden importante:** La ruta `/licencias/stats` debe ir ANTES de `/licencias/:chofer_id`, porque Express interpreta `stats` como un `chofer_id` si la ruta parametrizada va primero.

---

## PARTE 2 — ADMIN PANEL (React)

### 2.1 Modificar: `admin-panel/src/api/admin.js`

Agregar las llamadas a la API de licencias al final del archivo:

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

### 2.2 Nuevo archivo: `admin-panel/src/pages/Licencias.jsx`

```jsx
import { useState, useEffect } from 'react';
import { getLicencias, getLicenciasStats, registrarPago, cambiarEstadoLicencia, getProvincias } from '../api/admin';
import { Badge } from '../components/ui/Badge';
import { Table } from '../components/ui/Table';
import { formatDateShort } from '../utils/formatters';
import toast from 'react-hot-toast';

// Mapeo de colores para estados de licencia
const ESTADO_COLORS = {
  TRIAL_ACTIVO:    'bg-blue-100 text-blue-800',
  TRIAL_EXPIRADO:  'bg-red-100 text-red-700',
  ACTIVO:          'bg-green-100 text-green-800',
  SUSPENDIDO:      'bg-yellow-100 text-yellow-800',
  BLOQUEADO:       'bg-gray-200 text-gray-700',
};

const ESTADO_LABELS = {
  TRIAL_ACTIVO:    'Trial activo',
  TRIAL_EXPIRADO:  'Trial expirado',
  ACTIVO:          'Activo',
  SUSPENDIDO:      'Suspendido',
  BLOQUEADO:       'Bloqueado',
};

// — Componente de badge para licencias —
const LicenciaBadge = ({ estado }) => (
  <span className={`px-2 py-1 rounded-full text-xs font-semibold ${ESTADO_COLORS[estado] || 'bg-gray-100 text-gray-800'}`}>
    {ESTADO_LABELS[estado] || estado}
  </span>
);

// — Modal: Registrar pago —
const ModalPago = ({ chofer, onClose, onSuccess }) => {
  const [form, setForm] = useState({ monto: '', meses: 1, notas: '' });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    if (!form.monto || parseFloat(form.monto) <= 0) {
      toast.error('Ingresa un monto válido');
      return;
    }
    setLoading(true);
    try {
      await registrarPago(chofer.chofer_id, {
        monto: parseFloat(form.monto),
        meses: parseInt(form.meses),
        notas: form.notas || undefined
      });
      toast.success('Pago registrado correctamente');
      onSuccess();
      onClose();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Error al registrar pago');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md">
        <div className="p-6 border-b">
          <h2 className="text-lg font-bold text-gray-900">Registrar Pago</h2>
          <p className="text-sm text-gray-500 mt-1">{chofer.nombre} · @{chofer.username}</p>
        </div>
        <div className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Monto cobrado</label>
            <input
              type="number"
              min="0"
              step="0.01"
              placeholder="0.00"
              value={form.monto}
              onChange={e => setForm(f => ({ ...f, monto: e.target.value }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Meses a activar</label>
            <select
              value={form.meses}
              onChange={e => setForm(f => ({ ...f, meses: e.target.value }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
            >
              {[1,2,3,6,12].map(m => (
                <option key={m} value={m}>{m} {m === 1 ? 'mes' : 'meses'}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Notas (opcional)</label>
            <textarea
              placeholder="Método de pago, referencia, etc."
              value={form.notas}
              onChange={e => setForm(f => ({ ...f, notas: e.target.value }))}
              rows={2}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 resize-none"
            />
          </div>
        </div>
        <div className="p-6 border-t flex gap-3 justify-end">
          <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-900">
            Cancelar
          </button>
          <button
            onClick={handleSubmit}
            disabled={loading}
            className="px-5 py-2 bg-green-600 hover:bg-green-700 text-white text-sm font-medium rounded-lg disabled:opacity-50"
          >
            {loading ? 'Guardando...' : 'Confirmar pago'}
          </button>
        </div>
      </div>
    </div>
  );
};

// — Modal: Cambiar estado manualmente —
const ModalEstado = ({ chofer, onClose, onSuccess }) => {
  const [estado, setEstado] = useState('');
  const [notas, setNotas] = useState('');
  const [loading, setLoading] = useState(false);

  const estados = [
    { value: 'TRIAL_ACTIVO',   label: '🔵 Trial activo (extender 45 días)' },
    { value: 'ACTIVO',         label: '✅ Activar manualmente' },
    { value: 'SUSPENDIDO',     label: '⚠️ Suspender' },
    { value: 'BLOQUEADO',      label: '🚫 Bloquear' },
  ];

  const handleSubmit = async () => {
    if (!estado) { toast.error('Selecciona un estado'); return; }
    if (!confirm(`¿Confirmas cambiar la licencia de ${chofer.nombre} a "${ESTADO_LABELS[estado]}"?`)) return;
    setLoading(true);
    try {
      await cambiarEstadoLicencia(chofer.chofer_id, { estado, notas: notas || undefined });
      toast.success('Estado de licencia actualizado');
      onSuccess();
      onClose();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Error al cambiar estado');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md">
        <div className="p-6 border-b">
          <h2 className="text-lg font-bold text-gray-900">Cambiar Estado de Licencia</h2>
          <p className="text-sm text-gray-500 mt-1">{chofer.nombre} · @{chofer.username}</p>
        </div>
        <div className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Nuevo estado</label>
            <select
              value={estado}
              onChange={e => setEstado(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
            >
              <option value="">Seleccionar...</option>
              {estados.map(e => (
                <option key={e.value} value={e.value}>{e.label}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Motivo / Notas</label>
            <textarea
              placeholder="Razón del cambio de estado..."
              value={notas}
              onChange={e => setNotas(e.target.value)}
              rows={2}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 resize-none"
            />
          </div>
        </div>
        <div className="p-6 border-t flex gap-3 justify-end">
          <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-900">
            Cancelar
          </button>
          <button
            onClick={handleSubmit}
            disabled={loading}
            className="px-5 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-lg disabled:opacity-50"
          >
            {loading ? 'Guardando...' : 'Aplicar cambio'}
          </button>
        </div>
      </div>
    </div>
  );
};

// — Página principal —
const Licencias = () => {
  const [data, setData] = useState([]);
  const [stats, setStats] = useState(null);
  const [provincias, setProvincias] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState({ estado: '', provincia_id: '', alerta: '' });
  const [modalPago, setModalPago] = useState(null);    // chofer seleccionado
  const [modalEstado, setModalEstado] = useState(null); // chofer seleccionado

  const loadStats = () => {
    getLicenciasStats().then(setStats).catch(() => {});
  };

  const loadData = () => {
    setLoading(true);
    const params = {
      estado:      filter.estado || undefined,
      provincia_id: filter.provincia_id || undefined,
      alerta:      filter.alerta || undefined,
    };
    getLicencias(params)
      .then(setData)
      .catch(() => toast.error('Error al cargar licencias'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadStats();
    getProvincias().then(setProvincias).catch(() => {});
  }, []);

  useEffect(() => {
    loadData();
  }, [filter]);

  const handleSuccess = () => {
    loadData();
    loadStats();
  };

  const columns = [
    {
      key: 'chofer',
      label: 'Chofer',
      render: (item) => (
        <div>
          <p className="font-medium text-gray-900">{item.nombre}</p>
          <p className="text-xs text-gray-500">@{item.username} · {item.telefono}</p>
          <p className="text-xs text-gray-400">{item.provincia}</p>
        </div>
      )
    },
    {
      key: 'estado',
      label: 'Licencia',
      render: (item) => (
        <div className="space-y-1">
          <LicenciaBadge estado={item.estado} />
          {(item.estado === 'TRIAL_ACTIVO' || item.estado === 'ACTIVO') && (
            <p className="text-xs text-gray-500">
              {item.dias_restantes === 0 ? 'Vence hoy' : `${item.dias_restantes} días restantes`}
            </p>
          )}
        </div>
      )
    },
    {
      key: 'fechas',
      label: 'Fechas',
      render: (item) => (
        <div className="text-xs text-gray-500 space-y-0.5">
          <p>Trial: {formatDateShort(item.trial_inicio)} → {formatDateShort(item.trial_fin)}</p>
          {item.suscripcion_fin && (
            <p>Suscr.: hasta {formatDateShort(item.suscripcion_fin)}</p>
          )}
          {item.ultimo_pago && (
            <p className="text-green-600">Último pago: {formatDateShort(item.ultimo_pago)}</p>
          )}
        </div>
      )
    },
    {
      key: 'notas',
      label: 'Notas',
      render: (item) => (
        <p className="text-xs text-gray-500 max-w-xs truncate">{item.notas || '—'}</p>
      )
    },
    {
      key: 'acciones',
      label: 'Acciones',
      render: (item) => (
        <div className="flex gap-2 flex-wrap">
          <button
            onClick={() => setModalPago(item)}
            className="px-3 py-1 bg-green-600 hover:bg-green-700 text-white text-xs font-medium rounded-lg"
          >
            💰 Pago
          </button>
          <button
            onClick={() => setModalEstado(item)}
            className="px-3 py-1 bg-blue-600 hover:bg-blue-700 text-white text-xs font-medium rounded-lg"
          >
            ✏️ Estado
          </button>
        </div>
      )
    }
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Licencias de Uso</h1>
        <p className="text-sm text-gray-500 mt-1">Gestión de suscripciones y períodos de prueba de choferes</p>
      </div>

      {/* Stats */}
      {stats && (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
          {[
            { label: 'Trial activo',     value: stats.trial_activo,    color: 'bg-blue-50 border-blue-200 text-blue-800' },
            { label: 'Trial expirado',   value: stats.trial_expirado,  color: 'bg-red-50 border-red-200 text-red-800' },
            { label: 'Activos',          value: stats.activo,          color: 'bg-green-50 border-green-200 text-green-800' },
            { label: 'Suspendidos',      value: stats.suspendido,      color: 'bg-yellow-50 border-yellow-200 text-yellow-800' },
            { label: 'Bloqueados',       value: stats.bloqueado,       color: 'bg-gray-50 border-gray-200 text-gray-700' },
          ].map(s => (
            <div key={s.label} className={`border rounded-xl p-4 ${s.color}`}>
              <p className="text-2xl font-bold">{s.value ?? 0}</p>
              <p className="text-xs font-medium mt-1">{s.label}</p>
            </div>
          ))}
        </div>
      )}

      {/* Alerta de vencimientos próximos */}
      {stats && (parseInt(stats.trial_por_vencer) + parseInt(stats.suscripcion_por_vencer)) > 0 && (
        <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 flex items-center gap-3">
          <span className="text-2xl">⚠️</span>
          <div>
            <p className="font-semibold text-amber-800">Licencias próximas a vencer</p>
            <p className="text-sm text-amber-700">
              {stats.trial_por_vencer} trials y {stats.suscripcion_por_vencer} suscripciones vencen en los próximos 7 días.
              <button
                onClick={() => setFilter(f => ({ ...f, alerta: '1' }))}
                className="ml-2 underline font-medium"
              >
                Ver listado
              </button>
            </p>
          </div>
        </div>
      )}

      {/* Filtros */}
      <div className="flex flex-wrap gap-3">
        <select
          value={filter.estado}
          onChange={e => setFilter(f => ({ ...f, estado: e.target.value, alerta: '' }))}
          className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
        >
          <option value="">Todos los estados</option>
          <option value="TRIAL_ACTIVO">Trial activo</option>
          <option value="TRIAL_EXPIRADO">Trial expirado</option>
          <option value="ACTIVO">Activo</option>
          <option value="SUSPENDIDO">Suspendido</option>
          <option value="BLOQUEADO">Bloqueado</option>
        </select>

        <select
          value={filter.provincia_id}
          onChange={e => setFilter(f => ({ ...f, provincia_id: e.target.value }))}
          className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
        >
          <option value="">Todas las provincias</option>
          {provincias.map(p => (
            <option key={p.id} value={p.id}>{p.nombre}</option>
          ))}
        </select>

        <button
          onClick={() => setFilter({ estado: '', provincia_id: '', alerta: '' })}
          className="px-4 py-2 text-sm text-gray-600 hover:text-gray-900 border border-gray-300 rounded-lg"
        >
          Limpiar filtros
        </button>
      </div>

      {/* Tabla */}
      <Table columns={columns} data={data} loading={loading} emptyMessage="No hay licencias que coincidan con los filtros" />

      {/* Modales */}
      {modalPago && (
        <ModalPago
          chofer={modalPago}
          onClose={() => setModalPago(null)}
          onSuccess={handleSuccess}
        />
      )}
      {modalEstado && (
        <ModalEstado
          chofer={modalEstado}
          onClose={() => setModalEstado(null)}
          onSuccess={handleSuccess}
        />
      )}
    </div>
  );
};

export default Licencias;
```

---

### 2.3 Modificar: `admin-panel/src/App.jsx`

Agregar el import y la ruta de la nueva página:

```jsx
// Agregar este import con los demás:
import Licencias from './pages/Licencias';

// Agregar esta ruta dentro del bloque <Route element={<Layout />}>:
<Route path="/licencias" element={<Licencias />} />
```

---

### 2.4 Modificar: `admin-panel/src/components/layout/Sidebar.jsx`

Agregar la nueva entrada al array `menuItems`:

```jsx
// ANTES
const menuItems = [
  { path: '/dashboard',      label: 'Dashboard',       icon: '🏠' },
  { path: '/choferes',       label: 'Choferes',        icon: '🚗' },
  { path: '/usuarios',       label: 'Usuarios',        icon: '👥' },
  { path: '/solicitudes',    label: 'Solicitudes',     icon: '📋' },
  { path: '/notificaciones', label: 'Notificaciones',  icon: '🔔' },
];

// DESPUÉS
const menuItems = [
  { path: '/dashboard',      label: 'Dashboard',       icon: '🏠' },
  { path: '/choferes',       label: 'Choferes',        icon: '🚗' },
  { path: '/licencias',      label: 'Licencias',       icon: '🪪' },   // ← NUEVO
  { path: '/usuarios',       label: 'Usuarios',        icon: '👥' },
  { path: '/solicitudes',    label: 'Solicitudes',     icon: '📋' },
  { path: '/notificaciones', label: 'Notificaciones',  icon: '🔔' },
];
```

---

## RESUMEN DE ARCHIVOS MODIFICADOS/CREADOS

| Archivo | Tipo |
|---|---|
| `tebusco-api/src/controllers/licenciaController.js` | ✅ Nuevo |
| `tebusco-api/src/jobs/expireLicencias.js` | ✅ Nuevo |
| `tebusco-api/src/controllers/authController.js` | ✏️ Modificado (bloque `if tipo === 'chofer'`) |
| `tebusco-api/src/controllers/adminController.js` | ✏️ Modificado (3 líneas en `aprobarChofer`) |
| `tebusco-api/src/routes/admin.js` | ✏️ Modificado (import + 5 rutas nuevas) |
| `tebusco-api/src/index.js` | ✏️ Modificado (import + arranque del job) |
| `admin-panel/src/api/admin.js` | ✏️ Modificado (5 funciones nuevas al final) |
| `admin-panel/src/pages/Licencias.jsx` | ✅ Nuevo |
| `admin-panel/src/App.jsx` | ✏️ Modificado (import + ruta) |
| `admin-panel/src/components/layout/Sidebar.jsx` | ✏️ Modificado (1 entrada en menuItems) |
