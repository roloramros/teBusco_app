# Admin Panel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a React-based administration panel for the "Te Busco" platform to manage drivers, users, and notifications, communicating with a VPS-hosted API on port 8004.

**Architecture:** A modern SPA built with Vite and React 18, utilizing Tailwind CSS for styling and Axios for API communication. It uses a centralized AuthContext for session management and protected routing.

**Tech Stack:** React 18, Vite, Tailwind CSS, React Router v6, Axios, Recharts, React Hot Toast.

---

### Task 1: Project Scaffolding

**Files:**
- Create: `admin-panel/` (directory)
- Create: `admin-panel/.env`
- Create: `admin-panel/tailwind.config.js`

- [ ] **Step 1: Initialize Vite project**

Run: `npm create vite@latest admin-panel -- --template react`

- [ ] **Step 2: Install dependencies**

Run: `cd admin-panel && npm install && npm install -D tailwindcss postcss autoprefixer && npx tailwindcss init -p && npm install react-router-dom axios recharts react-hot-toast`

- [ ] **Step 3: Configure Tailwind CSS**

Modify `admin-panel/tailwind.config.js`:
```javascript
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
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
    },
  },
  plugins: [],
}
```

- [ ] **Step 4: Create .env file**

Create `admin-panel/.env`:
```env
VITE_API_URL=http://localhost:8004
```

- [ ] **Step 5: Verify dev server starts**

Run: `npm run dev`
Expected: Server starts without errors.

- [ ] **Step 6: Commit**

```bash
git add admin-panel/
git commit -m "chore: scaffold admin-panel with vite and tailwind"
```

---

### Task 2: API Client and Utilities

**Files:**
- Create: `admin-panel/src/api/axios.js`
- Create: `admin-panel/src/utils/formatters.js`

- [ ] **Step 1: Create Axios instance**

Create `admin-panel/src/api/axios.js`:
```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('admin_token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
```

- [ ] **Step 2: Create formatters**

Create `admin-panel/src/utils/formatters.js`:
```javascript
export const formatDate = (iso) => iso ? new Date(iso).toLocaleString('es-ES', {
  day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
}) : '—';

export const formatDateShort = (iso) => iso ? new Date(iso).toLocaleDateString('es-ES', {
  day: '2-digit', month: 'short', year: 'numeric'
}) : '—';

export const formatCurrency = (amount, moneda) => `${amount} ${moneda ?? ''}`;

export const formatRating = (rating) => rating ? `⭐ ${parseFloat(rating).toFixed(1)}` : 'Sin valoraciones';

export const shortId = (uuid) => uuid?.slice(0, 8).toUpperCase() ?? '—';

export const getInitials = (nombre) => nombre?.split(' ').slice(0, 2).map(n => n[0]).join('').toUpperCase() ?? '?';

export const statusLabel = (s) => ({
  disponible: 'Disponible', ocupado: 'Ocupado', inactivo: 'Inactivo',
  activa: 'Activa', en_proceso: 'En proceso', completada: 'Completada',
  cancelada: 'Cancelada', expirada: 'Expirada',
  pasajero: 'Pasajero', chofer: 'Chofer', admin: 'Admin'
})[s] ?? s;
```

- [ ] **Step 3: Commit**

```bash
git add admin-panel/src/api/axios.js admin-panel/src/utils/formatters.js
git commit -m "feat: add axios client and formatters"
```

---

### Task 3: Auth Context and Private Routes

**Files:**
- Create: `admin-panel/src/context/AuthContext.jsx`
- Create: `admin-panel/src/hooks/useAuth.js`
- Modify: `admin-panel/src/App.jsx`

- [ ] **Step 1: Implement AuthContext**

Create `admin-panel/src/context/AuthContext.jsx`:
```javascript
import { createContext, useState, useEffect } from 'react';
import api from '../api/axios';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [usuario, setUsuario] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('admin_token');
    if (token) {
      // Potentially verify token or just assume valid until 401
      setUsuario(JSON.parse(localStorage.getItem('admin_user')));
    }
    setLoading(false);
  }, []);

  const login = async (username, password) => {
    const { data } = await api.post('/api/auth/login', { username, password });
    if (data.data.usuario.tipo !== 'admin') {
      throw new Error('Acceso denegado. Solo administradores.');
    }
    localStorage.setItem('admin_token', data.data.token);
    localStorage.setItem('admin_user', JSON.stringify(data.data.usuario));
    setUsuario(data.data.usuario);
  };

  const logout = () => {
    localStorage.removeItem('admin_token');
    localStorage.removeItem('admin_user');
    setUsuario(null);
  };

  return (
    <AuthContext.Provider value={{ usuario, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};
```

- [ ] **Step 2: Create useAuth hook**

Create `admin-panel/src/hooks/useAuth.js`:
```javascript
import { useContext } from 'react';
import { AuthContext } from '../context/AuthContext';

export const useAuth = () => useContext(AuthContext);
```

- [ ] **Step 3: Setup routing in App.jsx**

Modify `admin-panel/src/App.jsx`:
```javascript
import { BrowserRouter as Router, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Toaster } from 'react-hot-toast';

const PrivateRoute = () => {
  const { usuario, loading } = useAuth();
  if (loading) return <div>Cargando...</div>;
  return usuario ? <Outlet /> : <Navigate to="/login" />;
};

function App() {
  return (
    <AuthProvider>
      <Router>
        <Toaster position="top-right" />
        <Routes>
          <Route path="/login" element={<div>Login Page (TBD)</div>} />
          <Route element={<PrivateRoute />}>
            <Route path="/" element={<Navigate to="/dashboard" />} />
            <Route path="/dashboard" element={<div>Dashboard (TBD)</div>} />
          </Route>
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
```

- [ ] **Step 4: Commit**

```bash
git add admin-panel/src/context/AuthContext.jsx admin-panel/src/hooks/useAuth.js admin-panel/src/App.jsx
git commit -m "feat: implement auth context and protected routes"
```

---

### Task 4: UI Components (Part 1)

**Files:**
- Create: `admin-panel/src/components/ui/StatCard.jsx`
- Create: `admin-panel/src/components/ui/Badge.jsx`
- Create: `admin-panel/src/components/ui/Spinner.jsx`

- [ ] **Step 1: Create StatCard component**

Create `admin-panel/src/components/ui/StatCard.jsx`:
```javascript
export const StatCard = ({ title, value, icon, color = 'blue', loading }) => {
  const colors = {
    blue: 'text-blue-600 bg-blue-50',
    green: 'text-green-600 bg-green-50',
    yellow: 'text-yellow-600 bg-yellow-50',
    red: 'text-red-600 bg-red-50',
    purple: 'text-purple-600 bg-purple-50',
  };

  return (
    <div className="bg-white p-6 rounded-xl border border-gray-100 shadow-sm">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium text-gray-500 uppercase tracking-wider">{title}</p>
          {loading ? (
            <div className="h-8 w-24 bg-gray-100 animate-pulse mt-1 rounded"></div>
          ) : (
            <p className="text-3xl font-bold text-gray-900 mt-1">{value}</p>
          )}
        </div>
        <div className={`p-3 rounded-lg ${colors[color]}`}>
          <span className="text-2xl">{icon}</span>
        </div>
      </div>
    </div>
  );
};
```

- [ ] **Step 2: Create Badge component**

Create `admin-panel/src/components/ui/Badge.jsx`:
```javascript
export const Badge = ({ status, children }) => {
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
  };

  return (
    <span className={`px-2 py-1 rounded-full text-xs font-semibold ${colors[status] || 'bg-gray-100 text-gray-800'}`}>
      {children}
    </span>
  );
};
```

- [ ] **Step 3: Create Spinner component**

Create `admin-panel/src/components/ui/Spinner.jsx`:
```javascript
export const Spinner = ({ size = 'md', className = '' }) => {
  const sizes = { sm: 'h-4 w-4', md: 'h-8 w-8', lg: 'h-12 w-12' };
  return (
    <div className={`animate-spin rounded-full border-2 border-brand-500 border-t-transparent ${sizes[size]} ${className}`}></div>
  );
};
```

- [ ] **Step 4: Commit**

```bash
git add admin-panel/src/components/ui/
git commit -m "feat: add base UI components"
```

---

### Task 5: Layout Components (Sidebar & Header)

**Files:**
- Create: `admin-panel/src/components/layout/Sidebar.jsx`
- Create: `admin-panel/src/components/layout/Header.jsx`
- Create: `admin-panel/src/components/layout/Layout.jsx`

- [ ] **Step 1: Create Sidebar**

Create `admin-panel/src/components/layout/Sidebar.jsx`:
```javascript
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

const menuItems = [
  { path: '/dashboard', label: 'Dashboard', icon: '🏠' },
  { path: '/choferes', label: 'Choferes', icon: '🚗' },
  { path: '/usuarios', label: 'Usuarios', icon: '👥' },
  { path: '/solicitudes', label: 'Solicitudes', icon: '📋' },
  { path: '/notificaciones', label: 'Notificaciones', icon: '🔔' },
];

export const Sidebar = ({ isOpen, onClose }) => {
  const { usuario, logout } = useAuth();

  return (
    <aside className={`fixed inset-y-0 left-0 z-50 w-64 bg-gray-900 text-white transform ${isOpen ? 'translate-x-0' : '-translate-x-full'} transition-transform lg:translate-x-0 lg:static lg:inset-0`}>
      <div className="flex flex-col h-full">
        <div className="p-6 flex items-center gap-3">
          <span className="text-3xl">🚕</span>
          <h1 className="text-xl font-bold">Te Busco Admin</h1>
        </div>
        
        <nav className="flex-1 px-4 space-y-2">
          {menuItems.map(item => (
            <NavLink
              key={item.path}
              to={item.path}
              onClick={onClose}
              className={({ isActive }) => `flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${isActive ? 'bg-brand-500 text-white' : 'text-gray-400 hover:bg-gray-800 hover:text-white'}`}
            >
              <span>{item.icon}</span>
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="p-4 border-t border-gray-800">
          <div className="flex items-center gap-3 mb-4">
            <div className="w-10 h-10 rounded-full bg-brand-500 flex items-center justify-center font-bold">
              {usuario?.nombre?.[0]}
            </div>
            <div>
              <p className="text-sm font-medium">{usuario?.nombre}</p>
              <p className="text-xs text-gray-400">Administrador</p>
            </div>
          </div>
          <button onClick={logout} className="w-full text-left text-sm text-red-400 hover:text-red-300 px-2 py-1">
            Cerrar sesión
          </button>
        </div>
      </div>
    </aside>
  );
};
```

- [ ] **Step 2: Create Header**

Create `admin-panel/src/components/layout/Header.jsx`:
```javascript
export const Header = ({ onMenuClick, title }) => (
  <header className="bg-white border-b border-gray-200 h-16 flex items-center justify-between px-6 lg:px-8">
    <div className="flex items-center gap-4">
      <button onClick={onMenuClick} className="lg:hidden text-gray-500 hover:text-gray-700">
        ☰
      </button>
      <h2 className="text-xl font-semibold text-gray-800">{title}</h2>
    </div>
  </header>
);
```

- [ ] **Step 3: Create Layout wrapper**

Create `admin-panel/src/components/layout/Layout.jsx`:
```javascript
import { useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Header } from './Header';

const titles = {
  '/dashboard': 'Dashboard',
  '/choferes': 'Gestión de Choferes',
  '/usuarios': 'Gestión de Usuarios',
  '/solicitudes': 'Monitoreo de Solicitudes',
  '/notificaciones': 'Enviar Notificaciones',
};

export const Layout = () => {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const location = useLocation();

  return (
    <div className="flex h-screen bg-gray-50">
      <Sidebar isOpen={isSidebarOpen} onClose={() => setIsSidebarOpen(false)} />
      <div className="flex-1 flex flex-col overflow-hidden">
        <Header onMenuClick={() => setIsSidebarOpen(true)} title={titles[location.pathname] || 'Panel'} />
        <main className="flex-1 overflow-x-hidden overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
```

- [ ] **Step 4: Commit**

```bash
git add admin-panel/src/components/layout/
git commit -m "feat: add layout components"
```

---

### Task 6: Login Page Implementation

**Files:**
- Create: `admin-panel/src/pages/Login.jsx`
- Modify: `admin-panel/src/App.jsx`

- [ ] **Step 1: Implement Login page**

Create `admin-panel/src/pages/Login.jsx`:
```javascript
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { Spinner } from '../components/ui/Spinner';
import toast from 'react-hot-toast';

const Login = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await login(username, password);
      toast.success('Bienvenido al panel');
      navigate('/dashboard');
    } catch (err) {
      toast.error(err.response?.data?.message || err.message || 'Error al iniciar sesión');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-900 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-8">
        <div className="text-center mb-8">
          <span className="text-5xl block mb-2">🚕</span>
          <h1 className="text-2xl font-bold text-gray-900">Te Busco Admin</h1>
          <p className="text-gray-500">Inicia sesión para continuar</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Usuario</label>
            <input
              type="text"
              required
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-transparent outline-none"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Contraseña</label>
            <input
              type="password"
              required
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-transparent outline-none"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          <button
            type="submit"
            disabled={loading}
            className="w-full bg-brand-500 hover:bg-brand-600 text-white font-bold py-2 rounded-lg transition-colors flex items-center justify-center gap-2"
          >
            {loading ? <Spinner size="sm" className="border-white" /> : 'Iniciar Sesión'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default Login;
```

- [ ] **Step 2: Update App.jsx with Login and Layout**

Modify `admin-panel/src/App.jsx`:
```javascript
import { BrowserRouter as Router, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Toaster } from 'react-hot-toast';
import { Layout } from './components/layout/Layout';
import Login from './pages/Login';

const PrivateRoute = () => {
  const { usuario, loading } = useAuth();
  if (loading) return <div className="flex h-screen items-center justify-center bg-gray-50 text-brand-500 font-bold">Cargando...</div>;
  return usuario ? <Outlet /> : <Navigate to="/login" />;
};

function App() {
  return (
    <AuthProvider>
      <Router>
        <Toaster position="top-right" />
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route element={<PrivateRoute />}>
            <Route element={<Layout />}>
              <Route path="/" element={<Navigate to="/dashboard" />} />
              <Route path="/dashboard" element={<div>Dashboard (TBD)</div>} />
              <Route path="/choferes" element={<div>Choferes (TBD)</div>} />
              <Route path="/usuarios" element={<div>Usuarios (TBD)</div>} />
              <Route path="/solicitudes" element={<div>Solicitudes (TBD)</div>} />
              <Route path="/notificaciones" element={<div>Notificaciones (TBD)</div>} />
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/dashboard" />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
```

- [ ] **Step 3: Commit**

```bash
git add admin-panel/src/pages/Login.jsx admin-panel/src/App.jsx
git commit -m "feat: implement login page and full navigation structure"
```

---

### Task 7: Dashboard Implementation

**Files:**
- Create: `admin-panel/src/api/admin.js`
- Create: `admin-panel/src/pages/Dashboard.jsx`

- [ ] **Step 1: Create admin API functions**

Create `admin-panel/src/api/admin.js`:
```javascript
import api from './axios';

export const getStats = () => api.get('/api/admin/stats').then(res => res.data.data);
export const getChoferes = (params) => api.get('/api/admin/choferes', { params }).then(res => res.data);
export const getChoferById = (id) => api.get(`/api/admin/choferes/${id}`).then(res => res.data.data);
export const aprobarChofer = (id) => api.post(`/api/admin/choferes/${id}/aprobar`).then(res => res.data);
export const rechazarChofer = (id, motivo) => api.post(`/api/admin/choferes/${id}/rechazar`, { motivo }).then(res => res.data);
export const getUsuarios = (params) => api.get('/api/admin/usuarios', { params }).then(res => res.data);
export const toggleUsuarioActivo = (id) => api.patch(`/api/admin/usuarios/${id}/toggle-activo`).then(res => res.data);
export const getSolicitudes = (params) => api.get('/api/admin/solicitudes', { params }).then(res => res.data);
export const broadcastNotification = (body) => api.post('/api/admin/notificaciones/broadcast', body).then(res => res.data);
```

- [ ] **Step 2: Implement Dashboard**

Create `admin-panel/src/pages/Dashboard.jsx`:
```javascript
import { useState, useEffect } from 'react';
import { getStats } from '../api/admin';
import { StatCard } from '../components/ui/StatCard';
import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from 'recharts';
import toast from 'react-hot-toast';

const Dashboard = () => {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getStats()
      .then(setStats)
      .catch(err => toast.error('Error al cargar estadísticas'))
      .finally(() => setLoading(false));
  }, []);

  const chartData = [
    { name: 'Activos', value: stats?.viajes_activos || 0, color: '#3b82f6' },
    { name: 'Completados', value: stats?.viajes_completados || 0, color: '#10b981' },
    { name: 'Cancelados', value: stats?.viajes_cancelados || 0, color: '#ef4444' },
  ];

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard title="Usuarios Totales" value={stats?.total_usuarios} icon="👥" loading={loading} color="blue" />
        <StatCard title="Choferes" value={stats?.total_choferes} icon="🚗" loading={loading} color="purple" />
        <StatCard title="Viajes Completados" value={stats?.viajes_completados} icon="✅" loading={loading} color="green" />
        <StatCard title="Viajes Activos" value={stats?.viajes_activos} icon="🔥" loading={loading} color="yellow" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-white p-6 rounded-xl border border-gray-100 shadow-sm h-80">
          <h3 className="text-lg font-bold mb-4">Distribución de Viajes</h3>
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie data={chartData} innerRadius={60} outerRadius={80} paddingAngle={5} dataKey="value">
                {chartData.map((entry, index) => <Cell key={`cell-${index}`} fill={entry.color} />)}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        </div>
        
        <div className="bg-white p-6 rounded-xl border border-gray-100 shadow-sm">
          <h3 className="text-lg font-bold mb-4">Resumen</h3>
          <div className="space-y-4">
            <div className="flex justify-between items-center pb-2 border-b border-gray-50">
              <span className="text-gray-500">Pendientes de Aprobación</span>
              <span className={`font-bold ${stats?.choferes_pendientes > 0 ? 'text-red-500' : 'text-gray-900'}`}>{stats?.choferes_pendientes}</span>
            </div>
            <div className="flex justify-between items-center pb-2 border-b border-gray-50">
              <span className="text-gray-500">Valoración Promedio</span>
              <span className="font-bold text-yellow-500">⭐ {stats?.valoracion_promedio}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
```

- [ ] **Step 3: Update route in App.jsx**

Modify `admin-panel/src/App.jsx` to import and use `Dashboard` component.

- [ ] **Step 4: Commit**

```bash
git add admin-panel/src/api/admin.js admin-panel/src/pages/Dashboard.jsx admin-panel/src/App.jsx
git commit -m "feat: implement dashboard and admin api functions"
```

---

### Task 8: Drivers Page and Management

**Files:**
- Create: `admin-panel/src/components/ui/Table.jsx`
- Create: `admin-panel/src/pages/Choferes.jsx`
- Modify: `admin-panel/src/App.jsx`

- [ ] **Step 1: Create Table component**

Create `admin-panel/src/components/ui/Table.jsx`:
```javascript
export const Table = ({ columns, data, loading, emptyMessage = 'No se encontraron datos' }) => {
  return (
    <div className="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-left">
          <thead className="bg-gray-50 border-b border-gray-100">
            <tr>
              {columns.map((col) => (
                <th key={col.key} className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                  {col.label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-50">
            {loading ? (
              [...Array(5)].map((_, i) => (
                <tr key={i} className="animate-pulse">
                  {columns.map((col) => (
                    <td key={col.key} className="px-6 py-4">
                      <div className="h-4 bg-gray-100 rounded w-full"></div>
                    </td>
                  ))}
                </tr>
              ))
            ) : data.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className="px-6 py-10 text-center text-gray-500">
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              data.map((item, i) => (
                <tr key={item.id || i} className="hover:bg-gray-50 transition-colors">
                  {columns.map((col) => (
                    <td key={col.key} className="px-6 py-4 text-sm text-gray-700">
                      {col.render ? col.render(item) : item[col.key]}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
```

- [ ] **Step 2: Implement Choferes page**

Create `admin-panel/src/pages/Choferes.jsx`:
```javascript
import { useState, useEffect } from 'react';
import { getChoferes, aprobarChofer, rechazarChofer } from '../api/admin';
import { Table } from '../components/ui/Table';
import { Badge } from '../components/ui/Badge';
import { formatRating, formatDateShort } from '../utils/formatters';
import toast from 'react-hot-toast';

const Choferes = () => {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState({ estado: '', pendiente: false });

  const loadChoferes = () => {
    setLoading(true);
    const params = { 
      estado: filter.estado || undefined,
      pendiente: filter.pendiente ? '1' : undefined
    };
    getChoferes(params)
      .then(res => setData(res.data))
      .catch(() => toast.error('Error al cargar choferes'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadChoferes();
  }, [filter]);

  const handleAprobar = async (id) => {
    if (!confirm('¿Confirmas la aprobación de este chofer?')) return;
    try {
      await aprobarChofer(id);
      toast.success('Chofer aprobado con éxito');
      loadChoferes();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Error al aprobar');
    }
  };

  const columns = [
    { 
      key: 'chofer', 
      label: 'Chofer',
      render: (item) => (
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center font-bold text-gray-500">
            {item.nombre?.[0]}
          </div>
          <div>
            <p className="font-medium text-gray-900">{item.nombre}</p>
            <p className="text-xs text-gray-500">@{item.username}</p>
          </div>
        </div>
      )
    },
    { key: 'telefono', label: 'Teléfono' },
    { key: 'provincia', label: 'Provincia' },
    { 
      key: 'estado', 
      label: 'Estado',
      render: (item) => (
        <Badge status={item.verificado ? item.estado : 'pendiente'}>
          {item.verificado ? item.estado : 'Pendiente'}
        </Badge>
      )
    },
    { key: 'calificacion_promedio', label: 'Calificación', render: (item) => formatRating(item.calificacion_promedio) },
    { key: 'fecha_registro', label: 'Registro', render: (item) => formatDateShort(item.fecha_registro) },
    {
      key: 'acciones',
      label: 'Acciones',
      render: (item) => (
        <div className="flex gap-2">
          {!item.verificado && (
            <button 
              onClick={() => handleAprobar(item.id)}
              className="px-3 py-1 bg-green-500 text-white text-xs rounded hover:bg-green-600 transition-colors"
            >
              Aprobar
            </button>
          )}
          <button className="px-3 py-1 bg-gray-100 text-gray-700 text-xs rounded hover:bg-gray-200 transition-colors">
            Ver
          </button>
        </div>
      )
    }
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap gap-4 items-center justify-between bg-white p-4 rounded-xl border border-gray-100 shadow-sm">
        <div className="flex gap-4 items-center">
          <select 
            className="px-4 py-2 border border-gray-200 rounded-lg outline-none focus:ring-2 focus:ring-brand-500"
            value={filter.estado}
            onChange={(e) => setFilter(prev => ({ ...prev, estado: e.target.value }))}
          >
            <option value="">Todos los estados</option>
            <option value="disponible">Disponible</option>
            <option value="ocupado">Ocupado</option>
            <option value="inactivo">Inactivo</option>
          </select>
          <label className="flex items-center gap-2 text-sm font-medium text-gray-700 cursor-pointer">
            <input 
              type="checkbox" 
              className="w-4 h-4 text-brand-500 rounded border-gray-300 focus:ring-brand-500"
              checked={filter.pendiente}
              onChange={(e) => setFilter(prev => ({ ...prev, pendiente: e.target.checked }))}
            />
            Solo pendientes
          </label>
        </div>
      </div>

      <Table columns={columns} data={data} loading={loading} />
    </div>
  );
};

export default Choferes;
```

- [ ] **Step 3: Update route in App.jsx**

Modify `admin-panel/src/App.jsx` to import `Choferes` and use it in the `/choferes` route.

- [ ] **Step 4: Commit**

```bash
git add admin-panel/src/components/ui/Table.jsx admin-panel/src/pages/Choferes.jsx admin-panel/src/App.jsx
git commit -m "feat: implement drivers management page and Table component"
```

---

### Task 9: Final Pages (Users, Trips, Notifications)

**Files:**
- Create: `admin-panel/src/pages/Usuarios.jsx`
- Create: `admin-panel/src/pages/Solicitudes.jsx`
- Create: `admin-panel/src/pages/Notificaciones.jsx`
- Modify: `admin-panel/src/App.jsx`

- [ ] **Step 1: Implement Users page**

Create `admin-panel/src/pages/Usuarios.jsx`:
```javascript
import { useState, useEffect } from 'react';
import { getUsuarios, toggleUsuarioActivo } from '../api/admin';
import { Table } from '../components/ui/Table';
import { Badge } from '../components/ui/Badge';
import { formatDateShort } from '../utils/formatters';
import toast from 'react-hot-toast';

const Usuarios = () => {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState({ tipo: '', activo: '' });

  const loadUsuarios = () => {
    setLoading(true);
    getUsuarios({ tipo: filter.tipo || undefined, activo: filter.activo || undefined })
      .then(res => setData(res.data))
      .catch(() => toast.error('Error al cargar usuarios'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadUsuarios();
  }, [filter]);

  const handleToggle = async (user) => {
    if (!confirm(`¿Deseas ${user.activo ? 'desactivar' : 'activar'} a ${user.nombre}?`)) return;
    try {
      await toggleUsuarioActivo(user.id);
      toast.success('Usuario actualizado');
      loadUsuarios();
    } catch (err) {
      toast.error('Error al actualizar usuario');
    }
  };

  const columns = [
    { 
      key: 'usuario', 
      label: 'Usuario',
      render: (item) => (
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center font-bold text-gray-500">
            {item.nombre?.[0]}
          </div>
          <div>
            <p className="font-medium text-gray-900">{item.nombre}</p>
            <p className="text-xs text-gray-500">@{item.username}</p>
          </div>
        </div>
      )
    },
    { key: 'tipo', label: 'Tipo', render: (item) => <Badge status={item.tipo}>{item.tipo}</Badge> },
    { key: 'telefono', label: 'Teléfono' },
    { key: 'municipio', label: 'Municipio' },
    { 
      key: 'activo', 
      label: 'Estado',
      render: (item) => (
        <button onClick={() => handleToggle(item)} className="focus:outline-none">
          <div className={`w-12 h-6 flex items-center rounded-full p-1 transition-colors ${item.activo ? 'bg-green-500' : 'bg-gray-300'}`}>
            <div className={`bg-white w-4 h-4 rounded-full shadow-sm transform transition-transform ${item.activo ? 'translate-x-6' : ''}`}></div>
          </div>
        </button>
      )
    },
    { key: 'fecha_registro', label: 'Registro', render: (item) => formatDateShort(item.fecha_registro) }
  ];

  return (
    <div className="space-y-6">
      <div className="flex gap-4 items-center bg-white p-4 rounded-xl border border-gray-100 shadow-sm">
        <select 
          className="px-4 py-2 border border-gray-200 rounded-lg outline-none"
          value={filter.tipo}
          onChange={(e) => setFilter(prev => ({ ...prev, tipo: e.target.value }))}
        >
          <option value="">Todos los tipos</option>
          <option value="pasajero">Pasajeros</option>
          <option value="chofer">Choferes</option>
        </select>
        <select 
          className="px-4 py-2 border border-gray-200 rounded-lg outline-none"
          value={filter.activo}
          onChange={(e) => setFilter(prev => ({ ...prev, activo: e.target.value }))}
        >
          <option value="">Todos los estados</option>
          <option value="1">Activos</option>
          <option value="0">Inactivos</option>
        </select>
      </div>
      <Table columns={columns} data={data} loading={loading} />
    </div>
  );
};

export default Usuarios;
```

- [ ] **Step 2: Implement Trips (Solicitudes) page**

Create `admin-panel/src/pages/Solicitudes.jsx`:
```javascript
import { useState, useEffect } from 'react';
import { getSolicitudes } from '../api/admin';
import { Table } from '../components/ui/Table';
import { Badge } from '../components/ui/Badge';
import { formatDate, formatCurrency, shortId } from '../utils/formatters';
import toast from 'react-hot-toast';

const Solicitudes = () => {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getSolicitudes()
      .then(res => setData(res.data))
      .catch(() => toast.error('Error al cargar solicitudes'))
      .finally(() => setLoading(false));
  }, []);

  const columns = [
    { key: 'id', label: 'ID', render: (item) => <code className="text-xs">{shortId(item.id)}</code> },
    { key: 'pasajero_nombre', label: 'Pasajero' },
    { key: 'chofer_nombre', label: 'Chofer', render: (item) => item.chofer_nombre || <span className="text-gray-400 italic text-xs">Sin asignar</span> },
    { key: 'origen_descripcion', label: 'Origen' },
    { key: 'destino_descripcion', label: 'Destino' },
    { key: 'precio', label: 'Precio', render: (item) => formatCurrency(item.precio_oferta, item.moneda) },
    { key: 'estado', label: 'Estado', render: (item) => <Badge status={item.estado}>{item.estado}</Badge> },
    { key: 'creada_en', label: 'Fecha', render: (item) => formatDate(item.creada_en) }
  ];

  return <Table columns={columns} data={data} loading={loading} />;
};

export default Solicitudes;
```

- [ ] **Step 3: Implement Notifications Broadcast page**

Create `admin-panel/src/pages/Notificaciones.jsx`:
```javascript
import { useState } from 'react';
import { broadcastNotification } from '../api/admin';
import { Spinner } from '../components/ui/Spinner';
import toast from 'react-hot-toast';

const Notificaciones = () => {
  const [form, setForm] = useState({ titulo: '', cuerpo: '', tipo_usuario: '' });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!confirm('¿Confirmas el envío de esta notificación masiva?')) return;
    setLoading(true);
    try {
      const res = await broadcastNotification(form);
      toast.success(`Enviadas: ${res.enviadas}, Fallidas: ${res.fallidas}`);
      setForm({ titulo: '', cuerpo: '', tipo_usuario: '' });
    } catch (err) {
      toast.error('Error al enviar notificación');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
      <form onSubmit={handleSubmit} className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100 space-y-6">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Destinatarios</label>
          <select 
            className="w-full px-4 py-2 border border-gray-200 rounded-lg outline-none focus:ring-2 focus:ring-brand-500"
            value={form.tipo_usuario}
            onChange={(e) => setForm(prev => ({ ...prev, tipo_usuario: e.target.value }))}
          >
            <option value="">Todos los usuarios</option>
            <option value="pasajero">Solo pasajeros</option>
            <option value="chofer">Solo choferes</option>
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Título</label>
          <input 
            type="text" 
            required 
            maxLength={60}
            className="w-full px-4 py-2 border border-gray-200 rounded-lg outline-none focus:ring-2 focus:ring-brand-500"
            value={form.titulo}
            onChange={(e) => setForm(prev => ({ ...prev, titulo: e.target.value }))}
          />
          <p className="text-right text-[10px] text-gray-400 mt-1">{form.titulo.length}/60</p>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Mensaje</label>
          <textarea 
            required 
            rows={4}
            maxLength={300}
            className="w-full px-4 py-2 border border-gray-200 rounded-lg outline-none focus:ring-2 focus:ring-brand-500 resize-none"
            value={form.cuerpo}
            onChange={(e) => setForm(prev => ({ ...prev, cuerpo: e.target.value }))}
          />
          <p className="text-right text-[10px] text-gray-400 mt-1">{form.cuerpo.length}/300</p>
        </div>
        <button 
          disabled={loading || !form.titulo || !form.cuerpo}
          className="w-full bg-brand-500 hover:bg-brand-600 text-white font-bold py-3 rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
        >
          {loading ? <Spinner size="sm" className="border-white" /> : '🚀 Enviar Notificación'}
        </button>
      </form>

      <div className="space-y-4">
        <h3 className="text-sm font-bold text-gray-500 uppercase tracking-widest">Vista previa</h3>
        <div className="bg-gray-100 p-6 rounded-2xl flex items-center justify-center min-h-[300px]">
          <div className="bg-[#1c1c1e] text-white p-4 rounded-2xl w-full max-w-sm shadow-2xl">
            <div className="flex items-center gap-2 mb-2">
              <span className="text-xs bg-gray-700 p-1 rounded">🚕</span>
              <span className="text-[10px] font-bold text-gray-400 uppercase">Te Busco • ahora</span>
            </div>
            <p className="font-bold text-sm">{form.titulo || 'Título de notificación'}</p>
            <p className="text-sm text-gray-300 mt-0.5 line-clamp-2">{form.cuerpo || 'Este es un ejemplo de cómo se verá el mensaje en el dispositivo del usuario.'}</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Notificaciones;
```

- [ ] **Step 4: Update routes in App.jsx**

Modify `admin-panel/src/App.jsx` to import all new pages and use them in their respective routes.

- [ ] **Step 5: Final verification and Readme**

Create `admin-panel/README.md`:
```markdown
# Te Busco Admin Panel

Panel de administración web para la plataforma de transporte Te Busco.

## Requisitos
- Node.js 18+
- API Te Busco corriendo (puerto 8004)

## Instalación
1. `cd admin-panel`
2. `npm install`
3. Configurar `.env` con la URL de la API

## Desarrollo
`npm run dev`

## Producción
1. `npm run build`
2. Servir la carpeta `dist/`
```

- [ ] **Step 6: Commit**

```bash
git add .
git commit -m "feat: complete implementation of all admin pages"
```

