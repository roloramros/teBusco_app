import api from './axios';

export const getStats = (params) => api.get('/api/admin/stats', { params }).then(res => res.data.data);
export const getProvincias = () => api.get('/api/geo/provincias').then(res => res.data.data);
export const getChoferes = (params) => api.get('/api/admin/choferes', { params }).then(res => res.data.data.data);
export const getChoferById = (id) => api.get(`/api/admin/choferes/${id}`).then(res => res.data.data);
export const aprobarChofer = (id) => api.post(`/api/admin/choferes/${id}/aprobar`).then(res => res.data.data);
export const rechazarChofer = (id, motivo) => api.post(`/api/admin/choferes/${id}/rechazar`, { motivo }).then(res => res.data.data);
export const getUsuarios = (params) => api.get('/api/admin/usuarios', { params }).then(res => res.data.data.data);
export const toggleUsuarioActivo = (id) => api.patch(`/api/admin/usuarios/${id}/toggle-activo`).then(res => res.data.data);
export const deleteUsuario = (id) => api.delete(`/api/admin/usuarios/${id}`).then(res => res.data.data);
export const getSolicitudes = (params) => api.get('/api/admin/solicitudes', { params }).then(res => res.data.data.data);
export const broadcastNotification = (body) => api.post('/api/admin/notificaciones/broadcast', body).then(res => res.data.data);

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
