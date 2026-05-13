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
