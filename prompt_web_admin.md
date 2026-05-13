# Prompt: Panel de Administración Web — Te Busco

Eres un desarrollador frontend senior especializado en React. Voy a darte el contexto
completo de una API existente y necesito que construyas desde cero una web app de
administración que la consuma.

---

## CONTEXTO DE LA API

API REST en Node.js corriendo en `http://localhost:3000` (configurable por `.env`).

### Formato de todas las respuestas:
```json
// Éxito
{ "ok": true, "message": "...", "data": { ... } }

// Error
{ "ok": false, "message": "Descripción del error" }
```

### Autenticación:
- Login: `POST /api/auth/login` — body: `{ username, password }`
- Respuesta exitosa incluye `data.token` (JWT) y `data.usuario` con campos:
  `{ id, nombre, username, tipo, verificado, activo }`
- Todas las rutas privadas requieren header: `Authorization: Bearer <token>`
- Si la API responde 401, el token expiró — hacer logout automático

### Enums del sistema:
```
tipo_usuario:       pasajero | chofer | admin
estado_chofer:      disponible | ocupado | inactivo
estado_solicitud:   activa | en_proceso | completada | cancelada | expirada
```

---

## ENDPOINTS DE ADMIN DISPONIBLES

Todos bajo `/api/admin/`, todos requieren JWT de usuario con `tipo = 'admin'`.

### `GET /api/admin/stats`
Responde con:
```json
{
  "total_usuarios": 120,
  "total_pasajeros": 95,
  "total_choferes": 25,
  "choferes_pendientes": 4,
  "choferes_activos": 18,
  "viajes_completados": 310,
  "viajes_activos": 7,
  "viajes_cancelados": 23,
  "valoracion_promedio": 4.72
}
```

### `GET /api/admin/choferes`
Query params opcionales: `estado`, `pendiente` ('1'/'true'), `page` (default 1), `limit` (default 20, máx 50).
Responde:
```json
{
  "data": [{
    "id", "estado", "calificacion_promedio", "total_viajes",
    "opera_interprovincial", "licencia_numero", "aprobado_en",
    "usuario_id", "nombre", "username", "telefono", "email",
    "foto_url", "activo", "verificado", "fecha_registro", "fcm_token",
    "provincia", "municipio"
  }],
  "total": 25, "page": 1, "limit": 20
}
```

### `GET /api/admin/choferes/:id`
Responde igual que el listado más:
```json
{
  "vehiculos": [{ "id", "marca", "modelo", "color", "placa", "foto_url" }],
  "valoraciones": [{ "estrellas", "comentario", "creada_en", "pasajero_nombre" }],
  "solicitudes_recientes": [{ "id", "origen_descripcion", "destino_descripcion", "precio_oferta", "moneda", "creada_en" }]
}
```

### `POST /api/admin/choferes/:id/aprobar`
Sin body. Responde `{ ok: true, message: 'Chofer aprobado con éxito' }`.
Errores posibles: 404 (no existe), 400 (ya procesado o no inactivo).

### `POST /api/admin/choferes/:id/rechazar`
Body opcional: `{ "motivo": "string" }`.
Responde `{ ok: true, message: 'Chofer rechazado' }`.

### `GET /api/admin/usuarios`
Query params opcionales: `tipo`, `activo` ('1'/'0'/'true'/'false'), `search` (busca en nombre/username/email/teléfono), `page`, `limit`.
Responde paginado igual que choferes. Campos por usuario:
```
id, nombre, username, telefono, email, tipo, foto_url, activo, verificado, fecha_registro, provincia, municipio
```

### `PATCH /api/admin/usuarios/:id/toggle-activo`
Sin body. Responde `{ ok: true, data: { activo: true|false }, message: '...' }`.
Errores: 404 (no existe), 403 (intentar desactivar a un admin).

### `GET /api/admin/solicitudes`
Query params opcionales: `estado`, `page`, `limit`.
Responde paginado. Campos por solicitud:
```
id, estado, origen_descripcion, destino_descripcion, precio_oferta, moneda,
creada_en, completada_en, pasajero_nombre (del pasajero), chofer_nombre (del chofer asignado, nullable)
```

### `POST /api/admin/notificaciones/broadcast`
Body:
```json
{
  "titulo": "string (requerido)",
  "cuerpo": "string (requerido)",
  "tipo_usuario": "pasajero | chofer (opcional)"
}
```
Responde: `{ "enviadas": 98, "fallidas": 2 }`.
Error 400 si falta título o cuerpo.

---

## STACK TECNOLÓGICO

- **React 18 + Vite** — `npm create vite@latest admin-panel -- --template react`
- **Tailwind CSS v3** — estilos utilitarios
- **React Router v6** — navegación SPA con rutas protegidas
- **Axios** — cliente HTTP con interceptores
- **Recharts** — gráficas del dashboard
- **React Hot Toast** — notificaciones de UI (`npm i react-hot-toast`)
- UI construida completamente a mano con Tailwind — sin librerías de componentes externas

---

## ESTRUCTURA DE CARPETAS

```
admin-panel/
  src/
    api/
      axios.js          ← instancia base con interceptores
      auth.js           ← login, logout
      admin.js          ← funciones para cada endpoint de /api/admin/*
    components/
      layout/
        Sidebar.jsx
        Header.jsx
        Layout.jsx       ← wrapper con Sidebar + Header + <Outlet />
      ui/
        StatCard.jsx
        Badge.jsx
        Table.jsx
        Pagination.jsx
        Modal.jsx
        Spinner.jsx
        EmptyState.jsx
      charts/
        TripsChart.jsx
    pages/
      Login.jsx
      Dashboard.jsx
      Choferes.jsx
      ChoferDetalle.jsx
      Usuarios.jsx
      Solicitudes.jsx
      Notificaciones.jsx
      NotFound.jsx
    context/
      AuthContext.jsx    ← proveedor de sesión global
    hooks/
      useAuth.js         ← consume AuthContext
    utils/
      formatters.js
    App.jsx              ← define el árbol de rutas
    main.jsx
  .env
  .env.example
  tailwind.config.js
  vite.config.js
```

---

## CONFIGURACIÓN

### .env.example:
```
VITE_API_URL=http://localhost:3000
```

### tailwind.config.js — colores personalizados:
```js
theme: {
  extend: {
    colors: {
      brand: {
        50:  '#fef9ee',
        100: '#fdf0d3',
        400: '#fbbf24',
        500: '#f59e0b',
        600: '#d97706',
        700: '#b45309',
      }
    }
  }
}
```

---

## IMPLEMENTACIÓN DETALLADA

### `api/axios.js`
- Instancia Axios con `baseURL: import.meta.env.VITE_API_URL`
- **Interceptor de request**: añade `Authorization: Bearer <token>` si existe `localStorage.getItem('admin_token')`
- **Interceptor de response**: si el status es 401, limpiar `localStorage` y hacer `window.location.href = '/login'`

### `api/admin.js`
Exportar una función por endpoint. Cada una recibe los parámetros necesarios y retorna `response.data.data`. Ejemplos:
```js
export const getStats = () => api.get('/api/admin/stats')
export const getChoferes = (params) => api.get('/api/admin/choferes', { params })
export const aprobarChofer = (id) => api.post(`/api/admin/choferes/${id}/aprobar`)
export const rechazarChofer = (id, motivo) => api.post(`/api/admin/choferes/${id}/rechazar`, { motivo })
export const toggleUsuarioActivo = (id) => api.patch(`/api/admin/usuarios/${id}/toggle-activo`)
export const broadcastNotification = (body) => api.post('/api/admin/notificaciones/broadcast', body)
// ... etc
```

### `context/AuthContext.jsx`
- Estado: `{ usuario, token, loading }`
- `login(username, password)`:
  1. Llamar a `POST /api/auth/login`
  2. Verificar `data.usuario.tipo === 'admin'` — si no, lanzar error `"Acceso denegado. Solo administradores."`
  3. Guardar `token` en `localStorage` bajo clave `'admin_token'`
  4. Guardar `usuario` en el estado del contexto
- `logout()`: limpiar localStorage, resetear estado, redirigir a `/login`
- Al montar: leer `localStorage` para restaurar sesión si existe token

### `App.jsx` — árbol de rutas:
```jsx
<AuthProvider>
  <Router>
    <Toaster position="top-right" />
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<PrivateRoute />}>
        <Route element={<Layout />}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/choferes" element={<Choferes />} />
          <Route path="/choferes/:id" element={<ChoferDetalle />} />
          <Route path="/usuarios" element={<Usuarios />} />
          <Route path="/solicitudes" element={<Solicitudes />} />
          <Route path="/notificaciones" element={<Notificaciones />} />
        </Route>
      </Route>
      <Route path="/" element={<Navigate to="/dashboard" />} />
      <Route path="*" element={<NotFound />} />
    </Routes>
  </Router>
</AuthProvider>
```

`PrivateRoute`: si no hay token en contexto, redirige a `/login`. Si hay token, renderiza `<Outlet />`.

---

## PÁGINAS

### `Login.jsx`
- Fondo gris oscuro, card centrada verticalmente
- Logo: icono de taxi 🚕 + texto "Te Busco Admin"
- Inputs: username y password (con toggle de visibilidad en password)
- Botón "Iniciar sesión" con estado de carga (spinner inline)
- Muestra error debajo del botón si las credenciales fallan o si el usuario no es admin
- Al hacer login exitoso, redirige a `/dashboard`

---

### `Dashboard.jsx`
Consume `GET /api/admin/stats` al montar.

**Fila 1 — 4 StatCards grandes:**
- 👥 Usuarios Totales → `total_usuarios`
- 🚗 Choferes → `total_choferes`
- ✅ Viajes Completados → `viajes_completados`
- 🔥 Viajes Activos → `viajes_activos` (badge azul)

**Fila 2 — 3 StatCards secundarias:**
- ⏳ Pendientes de Aprobación → `choferes_pendientes` (badge rojo si > 0, con enlace a `/choferes?pendiente=1`)
- ❌ Viajes Cancelados → `viajes_cancelados`
- ⭐ Valoración Promedio → `valoracion_promedio` (renderizar estrellas visuales)

**Fila 3 — 2 columnas:**
- Izquierda: `TripsChart` — gráfica de dona (Recharts `PieChart`) con los 5 estados de solicitudes, leyenda incluida, colores: activa=azul, en_proceso=amarillo, completada=verde, cancelada=rojo, expirada=gris
- Derecha: "Choferes pendientes" — lista con los primeros 5 choferes pendientes (nombre, teléfono, fecha de registro, botón "Ver") + botón "Ver todos" al pie

---

### `Choferes.jsx`
Consume `GET /api/admin/choferes` con los params activos.

**Barra de filtros:**
- Input de búsqueda por nombre/teléfono (filtro local sobre los datos cargados)
- Select estado: Todos | Disponible | Ocupado | Inactivo
- Checkbox / toggle "Solo pendientes"

**Tabla** con columnas:
| Chofer | Teléfono | Provincia | Estado | Calificación | Viajes | Aprobado | Acciones |

- "Chofer": foto circular (o avatar iniciales si no hay foto) + nombre + username
- "Estado": Badge — `disponible`=verde, `ocupado`=amarillo, `inactivo sin aprobación`=rojo con texto "Pendiente", `inactivo aprobado`=gris
- "Calificación": estrellas + número
- "Aprobado": fecha formateada o "—" si no aplica
- "Acciones": botón "Ver" siempre, botón "Aprobar" (verde) solo si pendiente, botón "Rechazar" (rojo) solo si pendiente

**Modal Aprobar**: "¿Confirmas la aprobación de [nombre]? Se le enviará una notificación push."
**Modal Rechazar**: mismo pero con textarea opcional para el motivo.
Tras la acción: toast de éxito y recargar lista.

**Paginación** al pie.

---

### `ChoferDetalle.jsx`
Consume `GET /api/admin/choferes/:id` al montar.

**Sección hero:**
- Foto grande (o avatar) | Nombre | Badge de estado | ⭐ calificación | Total viajes
- Si está pendiente: botones prominentes "✅ Aprobar" y "❌ Rechazar" en la parte superior derecha

**Grid de datos personales:**
Teléfono | Email | Licencia | Municipio/Provincia | Opera interprovincial (Sí/No) | Fecha registro | Verificado

**Sección Vehículos:**
Tarjetas horizontales: foto + marca modelo + color + placa
Si no tiene vehículos: EmptyState "Sin vehículos registrados"

**Sección Valoraciones recientes (últimas 5):**
Lista: avatar del pasajero (iniciales) | nombre | estrellas visuales | comentario | fecha
Si no hay: EmptyState

**Sección Últimas solicitudes completadas (últimas 10):**
Tabla simple: origen → destino | precio | fecha
Si no hay: EmptyState

Botón "← Volver" en la parte superior.

---

### `Usuarios.jsx`
Consume `GET /api/admin/usuarios`.

**Barra de filtros:**
- Input de búsqueda (search param → se envía a la API con debounce de 400ms)
- Select tipo: Todos | Pasajeros | Choferes
- Select estado: Todos | Activos | Inactivos

**Tabla** con columnas:
| Usuario | Tipo | Teléfono | Municipio | Estado | Registro | Acciones |

- "Usuario": foto circular (o iniciales) + nombre + username
- "Tipo": Badge — `pasajero`=azul, `chofer`=purple
- "Estado": toggle switch — al hacer clic abre Modal de confirmación, luego llama a `toggle-activo`, toast de resultado
- "Acciones": solo ícono de información por ahora (sin página de detalle de usuario)

**Paginación** al pie.

---

### `Solicitudes.jsx`
Consume `GET /api/admin/solicitudes`. Solo lectura.

**Filtro:** Select de estado (Todos + los 5 valores del enum).

**Tabla** con columnas:
| ID | Pasajero | Chofer | Origen | Destino | Precio | Estado | Fecha |

- "ID": primeros 8 chars del UUID en mayúsculas + `#`
- "Chofer": nombre o "Sin asignar" en gris
- "Precio": monto + moneda (ej: "250 CUP")
- "Estado": Badge con color por estado
- "Fecha": fecha formateada

**Paginación** al pie.

---

### `Notificaciones.jsx`
Consume `POST /api/admin/notificaciones/broadcast`.

**Formulario:**
- Select "Destinatarios": Todos los usuarios | Solo pasajeros | Solo choferes
- Input "Título" (requerido, máx 60 chars con contador)
- Textarea "Mensaje" (requerido, máx 300 chars con contador)
- Botón "Enviar notificación" (deshabilitado si faltan título o mensaje)

**Vista previa** en tiempo real (a la derecha o debajo en mobile):
Mockup de notificación push — rectángulo redondeado gris oscuro con ícono 🚕, título en negrita, cuerpo en texto pequeño, hora "ahora"

**Modal de confirmación** antes de enviar:
"Vas a enviar una notificación a [Todos los usuarios / Pasajeros / Choferes]:"
→ Título: "..." | Mensaje: "..."
Botón "Confirmar" y "Cancelar"

Tras envío exitoso: toast "✅ Notificación enviada a N usuarios (M fallidas)".

---

## COMPONENTES REUTILIZABLES

### `StatCard.jsx`
Props: `title`, `value`, `icon` (emoji o SVG), `color` ('blue'|'green'|'yellow'|'red'|'purple'|'gray'), `linkTo?`, `badge?` (número — muestra dot rojo si > 0)
Muestra skeleton loader si `value` es `undefined`.

### `Badge.jsx`
Props: `status` — mapeo automático a colores:
```js
const colors = {
  disponible:   'bg-green-100 text-green-800',
  ocupado:      'bg-yellow-100 text-yellow-800',
  inactivo:     'bg-gray-100 text-gray-600',
  pendiente:    'bg-red-100 text-red-700',
  activa:       'bg-blue-100 text-blue-800',
  en_proceso:   'bg-yellow-100 text-yellow-800',
  completada:   'bg-green-100 text-green-800',
  cancelada:    'bg-red-100 text-red-800',
  expirada:     'bg-gray-100 text-gray-500',
  pasajero:     'bg-blue-100 text-blue-700',
  chofer:       'bg-purple-100 text-purple-700',
}
```

### `Table.jsx`
Props: `columns` (array de `{ key, label, render? }`), `data`, `loading` (muestra 5 filas skeleton si true), `emptyMessage?`

### `Pagination.jsx`
Props: `page`, `total`, `limit`, `onChange(newPage)`
Muestra: "Mostrando X-Y de Z resultados" + botones Anterior/Siguiente + números de página (máx 5 visibles)

### `Modal.jsx`
Props: `open`, `onClose`, `title`, `children`, `onConfirm?`, `confirmLabel?` ('Confirmar'), `confirmVariant?` ('danger'|'success'|'primary'), `loading?`
Overlay oscuro, animación de entrada suave, cierre con Escape y click fuera.

### `Spinner.jsx`
SVG animado en `brand-500`. Props: `size` ('sm'|'md'|'lg'), `className?`

### `EmptyState.jsx`
Props: `icon?` (emoji), `title`, `description?`, `action?` (botón)

---

## `utils/formatters.js`

```js
// "12 may 2025, 3:40 PM" (en español)
export const formatDate = (iso) => new Date(iso).toLocaleString('es-ES', {
  day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
})

// "12 may 2025"
export const formatDateShort = (iso) => new Date(iso).toLocaleDateString('es-ES', {
  day: '2-digit', month: 'short', year: 'numeric'
})

// "250 CUP" | "12.50 USD"
export const formatCurrency = (amount, moneda) => `${amount} ${moneda ?? ''}`

// "⭐ 4.8" | "Sin valoraciones"
export const formatRating = (rating) => rating ? `⭐ ${parseFloat(rating).toFixed(1)}` : 'Sin valoraciones'

// "A1B2C3D4"
export const shortId = (uuid) => uuid?.slice(0, 8).toUpperCase() ?? '—'

// 'en_proceso' → 'En proceso'
export const statusLabel = (s) => ({
  disponible: 'Disponible', ocupado: 'Ocupado', inactivo: 'Inactivo',
  activa: 'Activa', en_proceso: 'En proceso', completada: 'Completada',
  cancelada: 'Cancelada', expirada: 'Expirada',
  pasajero: 'Pasajero', chofer: 'Chofer', admin: 'Admin'
})[s] ?? s

// Genera iniciales para avatares: "Carlos Pérez" → "CP"
export const getInitials = (nombre) => nombre?.split(' ').slice(0, 2).map(n => n[0]).join('').toUpperCase() ?? '?'
```

---

## SIDEBAR

```
Logo: 🚕 Te Busco Admin
——————————————————
🏠  Dashboard          /dashboard
🚗  Choferes           /choferes    ← badge rojo si choferes_pendientes > 0
👥  Usuarios           /usuarios
📋  Solicitudes        /solicitudes
🔔  Notificaciones     /notificaciones
——————————————————
[Avatar] Nombre admin
         Cerrar sesión
```

- El conteo de `choferes_pendientes` se obtiene al montar el Layout con `GET /api/admin/stats`
  y se refresca automáticamente cada 60 segundos con `setInterval`
- Ítem activo: borde izquierdo `brand-500` + fondo `gray-800` + texto blanco
- En mobile: sidebar colapsable con botón hamburguesa en el Header

---

## MANEJO DE ERRORES

- Todos los errores de la API muestran toast con `response.data.message` si existe, o mensaje genérico
- Errores de red: toast "Error de conexión. Verifica que la API esté disponible."
- Estados de carga en todos los botones de acción (deshabilitar + mostrar spinner)
- Las tablas muestran skeleton loader mientras cargan
- Páginas con `loading` inicial muestran spinner centrado

---

## REGLAS GENERALES

- No usar TypeScript — JavaScript puro con JSX
- No usar librerías de componentes externas (no MUI, no Ant Design, no Chakra)
- Hooks de React estándar: `useState`, `useEffect`, `useCallback`, `useRef`
- Cada página maneja su propio estado de carga, error y datos
- No crear store global (Redux/Zustand) — el único contexto global es `AuthContext`
- Responsive: funcional en desktop (1280px+) y tablet (768px+), mobile es secundario

---

## ENTREGABLES

1. Proyecto completo en carpeta `admin-panel/` listo para `npm install && npm run dev`
2. Todos los archivos de la estructura listada
3. `README.md` con:
   - Requisitos (Node 18+)
   - Pasos de instalación
   - Cómo crear el usuario admin en la BD
   - Variables de entorno necesarias
   - Comando de desarrollo y build

---

## ORDEN DE IMPLEMENTACIÓN SUGERIDO

1. Scaffolding: Vite + Tailwind + React Router + dependencias
2. `api/axios.js` + `api/auth.js` + `api/admin.js`
3. `AuthContext` + `useAuth` + `Login.jsx`
4. `App.jsx` con rutas + `PrivateRoute` + `Layout` + `Sidebar` + `Header`
5. Componentes UI base: `StatCard`, `Badge`, `Table`, `Pagination`, `Modal`, `Spinner`, `EmptyState`
6. `utils/formatters.js`
7. `Dashboard.jsx` + `TripsChart.jsx`
8. `Choferes.jsx` + `ChoferDetalle.jsx`
9. `Usuarios.jsx`
10. `Solicitudes.jsx`
11. `Notificaciones.jsx`
12. `NotFound.jsx` + pulido final de estilos
