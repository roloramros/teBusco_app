import { useState, useEffect, useCallback } from 'react';
import { getSolicitudes, getProvincias } from '../api/admin';
import { Table } from '../components/ui/Table';
import { Badge } from '../components/ui/Badge';
import { Pagination } from '../components/ui/Pagination';
import { formatDate, formatCurrency, shortId } from '../utils/formatters';
import toast from 'react-hot-toast';

const Solicitudes = () => {
  const [data, setData]       = useState([]);
  const [total, setTotal]     = useState(0);
  const [page, setPage]       = useState(1);
  const [loading, setLoading] = useState(true);
  const [provincias, setProvincias] = useState([]);
  const ITEMS_PER_PAGE = 20;

  const [filter, setFilter] = useState({
    estado:      '',
    provincia_id: '',
  });

  // Cargar provincias una sola vez
  useEffect(() => {
    getProvincias()
      .then(setProvincias)
      .catch(() => {});
  }, []);

  // Resetear a página 1 cuando cambian los filtros
  useEffect(() => {
    setPage(1);
  }, [filter]);

  // Cargar solicitudes cuando cambia página o filtros
  const loadSolicitudes = useCallback(() => {
    setLoading(true);
    getSolicitudes({
      estado:       filter.estado       || undefined,
      provincia_id: filter.provincia_id || undefined,
      page,
      limit: ITEMS_PER_PAGE,
    })
      .then(res => {
        setData(res.data);
        setTotal(res.total);
      })
      .catch(() => toast.error('Error al cargar solicitudes'))
      .finally(() => setLoading(false));
  }, [filter, page]);

  // Cargar solicitudes cuando cambia página o filtros
  useEffect(() => {
    loadSolicitudes();
  }, [loadSolicitudes]);

  const columns = [
    {
      key: 'id',
      label: 'ID',
      render: (item) => <code className="text-xs text-gray-500">{shortId(item.id)}</code>,
    },
    {
      key: 'pasajero_nombre',
      label: 'Pasajero',
      render: (item) => (
        <div className="flex flex-col">
          <span className="font-medium text-gray-900">{item.pasajero_nombre}</span>
          <a
            href={`tel:${item.pasajero_telefono}`}
            className="text-[11px] text-blue-600 hover:underline"
          >
            📞 {item.pasajero_telefono}
          </a>
        </div>
      ),
    },
    {
      key: 'chofer_nombre',
      label: 'Chofer',
      render: (item) =>
        item.chofer_nombre ? (
          <span className="text-gray-800">{item.chofer_nombre}</span>
        ) : (
          <span className="text-gray-400 italic text-xs">Sin asignar</span>
        ),
    },
    { key: 'origen_descripcion',  label: 'Origen'  },
    { key: 'destino_descripcion', label: 'Destino' },
    {
      key: 'precio',
      label: 'Precio',
      render: (item) => formatCurrency(item.precio_oferta, item.moneda),
    },
    {
      key: 'estado',
      label: 'Estado',
      render: (item) => <Badge status={item.estado}>{item.estado}</Badge>,
    },
    {
      key: 'creada_en',
      label: 'Fecha',
      render: (item) => formatDate(item.creada_en),
    },
  ];

  return (
    <div className="space-y-4">

      {/* Título */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Solicitudes</h1>
        <p className="text-sm text-gray-500 mt-1">
          {total > 0 ? `${total} solicitudes en total` : 'Sin solicitudes'}
        </p>
      </div>

      {/* Filtros */}
      <div className="flex flex-wrap gap-3">
        <select
          value={filter.estado}
          onChange={e => setFilter(f => ({ ...f, estado: e.target.value }))}
          className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        >
          <option value="">Todos los estados</option>
          <option value="activa">Activa</option>
          <option value="en_proceso">En proceso</option>
          <option value="completada">Completada</option>
          <option value="cancelada">Cancelada</option>
          <option value="expirada">Expirada</option>
        </select>

        <select
          value={filter.provincia_id}
          onChange={e => setFilter(f => ({ ...f, provincia_id: e.target.value }))}
          className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        >
          <option value="">Todas las provincias</option>
          {provincias.map(p => (
            <option key={p.id} value={p.id}>{p.nombre}</option>
          ))}
        </select>

        <button
          onClick={() => setFilter({ estado: '', provincia_id: '' })}
          className="px-4 py-2 text-sm text-gray-600 hover:text-gray-900 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
        >
          Limpiar filtros
        </button>
      </div>

      {/* Tabla */}
      <Table
        columns={columns}
        data={data}
        loading={loading}
        emptyMessage="No hay solicitudes que coincidan con los filtros"
      />

      {/* Paginación */}
      <Pagination
        currentPage={page}
        totalItems={total}
        itemsPerPage={ITEMS_PER_PAGE}
        onPageChange={setPage}
      />

    </div>
  );
};

export default Solicitudes;
