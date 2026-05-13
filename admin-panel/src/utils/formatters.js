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
