# Design Spec: Admin Panel Web — Te Busco

Este documento detalla la arquitectura y el diseño para el panel de administración web de "Te Busco", una plataforma de transporte. La web app consumirá una API REST existente y se desplegará en un VPS.

## 1. Contexto y Objetivos
- **Propósito**: Gestionar choferes, usuarios, solicitudes y enviar notificaciones broadcast.
- **Usuario final**: Administradores del sistema.
- **Entorno**: Despliegue en VPS, consumiendo la API en el puerto 8004.

## 2. Stack Tecnológico
- **Frontend**: React 18 (Vite) + JavaScript.
- **Estilos**: Tailwind CSS v3 (sin librerías de componentes externas).
- **Navegación**: React Router v6.
- **HTTP**: Axios con interceptores para JWT y manejo de errores 401.
- **Gráficas**: Recharts.
- **Feedback**: React Hot Toast.

## 3. Arquitectura del Sistema

### 3.1. Estructura de Carpetas
```
admin-panel/
  src/
    api/             # Instancia de Axios y funciones de endpoint
    components/      # UI, Layout, Charts (reutilizables)
    context/         # AuthContext para sesión global
    hooks/           # useAuth
    pages/           # Vistas de la aplicación
    utils/           # Formateadores y constantes
    App.jsx          # Definición de rutas
    main.jsx         # Punto de entrada
```

### 3.2. Gestión de Sesión
- **AuthContext**: Almacena `usuario`, `token` y estado de `loading`.
- **Persistencia**: `localStorage` bajo la clave `admin_token`.
- **Protección**: Las rutas privadas redirigirán a `/login` si no hay un token válido.

## 4. Diseño de Interfaz (UI)

### 4.1. Layout
- **Sidebar**: Menú lateral oscuro con navegación principal y contador de choferes pendientes (refresco cada 60s).
- **Header**: Barra superior con título de la página y botón de colapso para mobile.
- **Main Content**: Área central con fondo gris claro (`bg-gray-50`) para resaltar las cards.

### 4.2. Colores (Brand)
```js
brand: {
  50:  '#fef9ee',
  100: '#fdf0d3',
  400: '#fbbf24', // Primario interactivo
  500: '#f59e0b', // Color base Te Busco
  600: '#d97706',
  700: '#b45309',
}
```

## 5. Páginas y Funcionalidades

### 5.1. Dashboard
- **Stats**: 4 cards principales (Usuarios, Choferes, Viajes Completados, Viajes Activos).
- **Gráfica**: `PieChart` con distribución de estados de solicitudes.
- **Acceso rápido**: Lista de los 5 choferes pendientes de aprobación más recientes.

### 5.2. Gestión de Choferes
- **Listado**: Tabla paginada con filtros por estado y búsqueda.
- **Detalle**: Vista profunda del chofer, incluyendo sus vehículos, valoraciones y solicitudes recientes.
- **Acciones**: Aprobar/Rechazar choferes con confirmación (Modal).

### 5.3. Gestión de Usuarios
- **Listado**: Tabla paginada de todos los usuarios (pasajeros y choferes).
- **Control**: Toggle para activar/desactivar cuentas de usuario.

### 5.4. Solicitudes
- **Historial**: Tabla de solo lectura de todas las solicitudes con filtros por estado.

### 5.5. Notificaciones
- **Broadcast**: Formulario para enviar mensajes push a grupos específicos (Todos, Choferes, Pasajeros).
- **Preview**: Mockup visual de cómo se verá la notificación en un móvil.

## 6. Configuración de Entorno (.env)
```env
VITE_API_URL=http://<vps-ip-o-dominio>:8004
```

## 7. Plan de Verificación
- **Login**: Probar credenciales válidas e inválidas, y verificar que solo admins entren.
- **API**: Confirmar que los interceptores añaden el Bearer token correctamente.
- **Responsive**: Verificar que el sidebar se oculte en pantallas pequeñas.
- **Acciones**: Validar que aprobar/rechazar choferes actualice la lista y muestre toast.
