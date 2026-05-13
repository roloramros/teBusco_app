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
