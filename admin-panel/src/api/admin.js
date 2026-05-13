import api from './axios';

export const getStats = (params) => api.get('/api/admin/stats', { params }).then(res => res.data.data);
export const getProvincias = () => api.get('/api/geo/provincias').then(res => res.data.data);
export const getChoferes = (params) => api.get('/api/admin/choferes', { params }).then(res => res.data.data.data);
export const getChoferById = (id) => api.get(`/api/admin/choferes/${id}`).then(res => res.data.data);
export const aprobarChofer = (id) => api.post(`/api/admin/choferes/${id}/aprobar`).then(res => res.data.data);
export const rechazarChofer = (id, motivo) => api.post(`/api/admin/choferes/${id}/rechazar`, { motivo }).then(res => res.data.data);
export const getUsuarios = (params) => api.get('/api/admin/usuarios', { params }).then(res => res.data.data.data);
export const toggleUsuarioActivo = (id) => api.patch(`/api/admin/usuarios/${id}/toggle-activo`).then(res => res.data.data);
export const getSolicitudes = (params) => api.get('/api/admin/solicitudes', { params }).then(res => res.data.data.data);
export const broadcastNotification = (body) => api.post('/api/admin/notificaciones/broadcast', body).then(res => res.data.data);
