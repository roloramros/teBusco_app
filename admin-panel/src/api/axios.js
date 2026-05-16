import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
});

const STORAGE_PREFIX = import.meta.env.VITE_STORAGE_PREFIX || 'prod';

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(`${STORAGE_PREFIX}_admin_token`);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(`${STORAGE_PREFIX}_admin_token`);
      localStorage.removeItem(`${STORAGE_PREFIX}_admin_user`);
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
