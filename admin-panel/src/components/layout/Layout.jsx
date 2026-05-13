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
