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
