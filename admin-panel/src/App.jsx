import { BrowserRouter as Router, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { useAuth } from './hooks/useAuth';
import { Toaster } from 'react-hot-toast';
import { Layout } from './components/layout/Layout';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Choferes from './pages/Choferes';
import Usuarios from './pages/Usuarios';
import Solicitudes from './pages/Solicitudes';
import Notificaciones from './pages/Notificaciones';

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
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/choferes" element={<Choferes />} />
              <Route path="/usuarios" element={<Usuarios />} />
              <Route path="/solicitudes" element={<Solicitudes />} />
              <Route path="/notificaciones" element={<Notificaciones />} />
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/dashboard" />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
