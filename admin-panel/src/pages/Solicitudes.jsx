import { useState, useEffect } from 'react';
import { getSolicitudes } from '../api/admin';
import { Table } from '../components/ui/Table';
import { Badge } from '../components/ui/Badge';
import { formatDate, formatCurrency, shortId } from '../utils/formatters';
import toast from 'react-hot-toast';

const Solicitudes = () => {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getSolicitudes()
      .then(setData)
      .catch(() => toast.error('Error al cargar solicitudes'))
      .finally(() => setLoading(false));
  }, []);

  const columns = [
    { key: 'id', label: 'ID', render: (item) => <code className="text-xs">{shortId(item.id)}</code> },
    { 
      key: 'pasajero_nombre', 
      label: 'Pasajero', 
      render: (item) => (
        <div className="flex flex-col">
          <span className="font-medium">{item.pasajero_nombre}</span>
          <a href={`tel:${item.pasajero_telefono}`} className="text-[10px] text-brand-600 hover:underline flex items-center gap-1">
            📞 {item.pasajero_telefono}
          </a>
        </div>
      )
    },
    { key: 'chofer_nombre', label: 'Chofer', render: (item) => item.chofer_nombre || <span className="text-gray-400 italic text-xs">Sin asignar</span> },
    { key: 'origen_descripcion', label: 'Origen' },
    { key: 'destino_descripcion', label: 'Destino' },
    { key: 'precio', label: 'Precio', render: (item) => formatCurrency(item.precio_oferta, item.moneda) },
    { key: 'estado', label: 'Estado', render: (item) => <Badge status={item.estado}>{item.estado}</Badge> },
    { key: 'creada_en', label: 'Fecha', render: (item) => formatDate(item.creada_en) }
  ];

  return <Table columns={columns} data={data} loading={loading} />;
};

export default Solicitudes;
