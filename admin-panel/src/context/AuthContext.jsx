import { createContext, useState, useEffect } from 'react';
import api from '../api/axios';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [usuario, setUsuario] = useState(null);
  const [loading, setLoading] = useState(true);
  const STORAGE_PREFIX = import.meta.env.VITE_STORAGE_PREFIX || 'prod';

  useEffect(() => {
    const token = localStorage.getItem(`${STORAGE_PREFIX}_admin_token`);
    if (token) {
      try {
        const storedUser = localStorage.getItem(`${STORAGE_PREFIX}_admin_user`);
        if (storedUser) {
          setUsuario(JSON.parse(storedUser));
        }
      } catch (e) {
        console.error('Error parsing stored user', e);
      }
    }
    setLoading(false);
  }, [STORAGE_PREFIX]);

  const login = async (identificador, password) => {
    const { data } = await api.post('/api/auth/login', { identificador, password });
    if (data.data.usuario.tipo !== 'admin') {
      throw new Error('Acceso denegado. Solo administradores.');
    }
    localStorage.setItem(`${STORAGE_PREFIX}_admin_token`, data.data.token);
    localStorage.setItem(`${STORAGE_PREFIX}_admin_user`, JSON.stringify(data.data.usuario));
    setUsuario(data.data.usuario);
  };

  const logout = () => {
    localStorage.removeItem(`${STORAGE_PREFIX}_admin_token`);
    localStorage.removeItem(`${STORAGE_PREFIX}_admin_user`);
    setUsuario(null);
  };

  return (
    <AuthContext.Provider value={{ usuario, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};
