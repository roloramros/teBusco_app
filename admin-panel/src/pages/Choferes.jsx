import { useState, useEffect } from 'react';
import { getChoferes, aprobarChofer, getProvincias } from '../api/admin';
import { Table } from '../components/ui/Table';
import { Badge } from '../components/ui/Badge';
import { formatRating, formatDateShort } from '../utils/formatters';
import toast from 'react-hot-toast';

const Choferes = () => {
  const [data, setData] = useState([]);
  const [provincias, setProvincias] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState({ estado: '', pendiente: false, provincia_id: '' });

  const loadInitialData = async () => {
    try {
      const provs = await getProvincias();
      setProvincias(provs);
    } catch (err) {
      toast.error('Error al cargar provincias');
    }
  };

  const loadChoferes = () => {
    setLoading(true);
    const params = { 
      estado: filter.estado || undefined,
      pendiente: filter.pendiente ? '1' : undefined,
      provincia_id: filter.provincia_id || undefined
    };
    getChoferes(params)
      .then(setData)
      .catch(() => toast.error('Error al cargar choferes'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadInitialData();
  }, []);

  useEffect(() => {
    loadChoferes();
  }, [filter]);

  const handleAprobar = async (id) => {
    if (!confirm('¿Confirmas la aprobación de este chofer?')) return;
    try {
      await aprobarChofer(id);
      toast.success('Chofer aprobado con éxito');
      loadChoferes();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Error al aprobar');
    }
  };

  const columns = [
    { 
      key: 'chofer', 
      label: 'Chofer',
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
    { key: 'telefono', label: 'Teléfono' },
    { key: 'provincia', label: 'Provincia' },
    { 
      key: 'estado', 
      label: 'Estado',
      render: (item) => (
        <Badge status={item.verificado ? item.estado : 'pendiente'}>
          {item.verificado ? item.estado : 'Pendiente'}
        </Badge>
      )
    },
    { key: 'calificacion_promedio', label: 'Calificación', render: (item) => formatRating(item.calificacion_promedio) },
    { key: 'fecha_registro', label: 'Registro', render: (item) => formatDateShort(item.fecha_registro) },
    {
      key: 'acciones',
      label: 'Acciones',
      render: (item) => (
        <div className="flex gap-2">
          {!item.verificado && (
            <button 
              onClick={() => handleAprobar(item.id)}
              className="px-3 py-1 bg-green-500 text-white text-xs rounded hover:bg-green-600 transition-colors"
            >
              Aprobar
            </button>
          )}
          <button className="px-3 py-1 bg-gray-100 text-gray-700 text-xs rounded hover:bg-gray-200 transition-colors">
            Ver
          </button>
        </div>
      )
    }
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap gap-4 items-center justify-between bg-white p-4 rounded-xl border border-gray-100 shadow-sm">
        <div className="flex gap-4 items-center">
          <select 
            className="px-4 py-2 border border-gray-200 rounded-lg outline-none focus:ring-2 focus:ring-brand-500"
            value={filter.estado}
            onChange={(e) => setFilter(prev => ({ ...prev, estado: e.target.value }))}
          >
            <option value="">Todos los estados</option>
            <option value="disponible">Disponible</option>
            <option value="ocupado">Ocupado</option>
            <option value="inactivo">Inactivo</option>
          </select>
          <select 
            className="px-4 py-2 border border-gray-200 rounded-lg outline-none focus:ring-2 focus:ring-brand-500"
            value={filter.provincia_id}
            onChange={(e) => setFilter(prev => ({ ...prev, provincia_id: e.target.value }))}
          >
            <option value="">Todas las provincias</option>
            {provincias.map(p => (
              <option key={p.id} value={p.id}>{p.nombre}</option>
            ))}
          </select>
          <label className="flex items-center gap-2 text-sm font-medium text-gray-700 cursor-pointer">
            <input 
              type="checkbox" 
              className="w-4 h-4 text-brand-500 rounded border-gray-300 focus:ring-brand-500"
              checked={filter.pendiente}
              onChange={(e) => setFilter(prev => ({ ...prev, pendiente: e.target.checked }))}
            />
            Solo pendientes
          </label>
        </div>
      </div>

      <Table columns={columns} data={data} loading={loading} />
    </div>
  );
};

export default Choferes;
