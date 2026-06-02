import { useState, useEffect } from 'react';
import { getUsuarios, toggleUsuarioActivo, getProvincias, deleteUsuario, notificarUsuario } from '../api/admin';
import { Table } from '../components/ui/Table';
import { Badge } from '../components/ui/Badge';
import { Pagination } from '../components/ui/Pagination';
import { formatDateShort } from '../utils/formatters';
import toast from 'react-hot-toast';

const Usuarios = () => {
 const [data, setData] = useState([]);
 const [total, setTotal] = useState(0);
 const [page, setPage] = useState(1);
 const ITEMS_PER_PAGE = 20;

 const [provincias, setProvincias] = useState([]);
 const [loading, setLoading] = useState(true);
 const [filter, setFilter] = useState({ tipo: '', activo: '', provincia_id: '', search: '' });

 const [modalNotif, setModalNotif] = useState(null); // usuario seleccionado
 const [notifForm, setNotifForm] = useState({ titulo: '', cuerpo: '' });
 const [enviando, setEnviando] = useState(false);

 const handleDelete = async (user) => {
  if (!confirm(`¿Estás seguro de que deseas eliminar a ${user.nombre}? Esta acción no se puede deshacer.`)) return;
  try {
   await deleteUsuario(user.id);
   toast.success('Usuario eliminado correctamente');
   loadUsuarios();
  } catch {
   toast.error('Error al eliminar usuario');
  }
 };

 const handleNotificar = async () => {
  if (!notifForm.titulo.trim() || !notifForm.cuerpo.trim()) {
    toast.error('El título y el mensaje son obligatorios');
    return;
  }
  setEnviando(true);
  try {
    await notificarUsuario(modalNotif.id, {
      titulo: notifForm.titulo.trim(),
      cuerpo: notifForm.cuerpo.trim()
    });
    toast.success(`Notificación enviada a ${modalNotif.nombre}`);
    setModalNotif(null);
    setNotifForm({ titulo: '', cuerpo: '' });
  } catch (err) {
    toast.error(err.response?.data?.message || 'Error al enviar la notificación');
  } finally {
    setEnviando(false);
  }
 };

  const loadInitialData = async () => {
    try {
      const provs = await getProvincias();
      setProvincias(provs);
    } catch (err) {
      toast.error('Error al cargar provincias');
    }
  };

  const loadUsuarios = () => {
    setLoading(true);
    getUsuarios({ 
      tipo: filter.tipo || undefined, 
      activo: filter.activo || undefined,
      provincia_id: filter.provincia_id || undefined,
      search: filter.search || undefined,
      page: page,
      limit: ITEMS_PER_PAGE
    })
      .then(res => {
        setData(res.data);
        setTotal(res.total);
      })
      .catch(() => toast.error('Error al cargar usuarios'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadInitialData();
  }, []);

  useEffect(() => {
    setPage(1); // Resetear a la primera página si cambian los filtros
  }, [filter]);

  useEffect(() => {
    loadUsuarios();
  }, [filter, page]);

  const handleToggle = async (user) => {
    if (!confirm(`¿Deseas ${user.activo ? 'desactivar' : 'activar'} a ${user.nombre}?`)) return;
    try {
      await toggleUsuarioActivo(user.id);
      toast.success('Usuario actualizado');
      loadUsuarios();
    } catch {
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
    { key: 'fecha_registro', label: 'Registro', render: (item) => formatDateShort(item.fecha_registro) },
{ key: 'ultimo_acceso', label: 'Último acceso', render: (item) => item.ultimo_acceso ? formatDateShort(item.ultimo_acceso) : 'Nunca' },
{
 key: 'acciones',
 label: 'Acciones',
 render: (item) => (
   <div className="flex gap-2">
     <button
       onClick={() => {
         setModalNotif(item);
         setNotifForm({ titulo: '', cuerpo: '' });
       }}
       className="px-3 py-1 bg-blue-500 text-white rounded hover:bg-blue-600 transition-colors text-sm"
       title="Enviar notificación push"
     >
       🔔 Notificar
     </button>
     <button
       onClick={() => handleDelete(item)}
       className="px-3 py-1 bg-red-500 text-white rounded hover:bg-red-600 transition-colors text-sm"
     >
       Eliminar
     </button>
   </div>
 )
}
  ];

  return (
    <div className="space-y-6">
      <div className="flex gap-4 items-center bg-white p-4 rounded-xl border border-gray-100 shadow-sm flex-wrap">
        <input 
          type="text"
          placeholder="Buscar por nombre, usuario..."
          className="px-4 py-2 border border-gray-200 rounded-lg outline-none focus:ring-2 focus:ring-blue-500 text-sm w-64"
          value={filter.search}
          onChange={(e) => setFilter(prev => ({ ...prev, search: e.target.value }))}
        />
        <select 
          className="px-4 py-2 border border-gray-200 rounded-lg outline-none text-sm"
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
        <select 
          className="px-4 py-2 border border-gray-200 rounded-lg outline-none"
          value={filter.provincia_id}
          onChange={(e) => setFilter(prev => ({ ...prev, provincia_id: e.target.value }))}
        >
          <option value="">Todas las provincias</option>
          {provincias.map(p => (
            <option key={p.id} value={p.id}>{p.nombre}</option>
          ))}
        </select>
      </div>
      <Table columns={columns} data={data} loading={loading} />

      <Pagination 
        currentPage={page} 
        totalItems={total} 
        itemsPerPage={ITEMS_PER_PAGE} 
        onPageChange={setPage} 
      />

      {/* Modal: Enviar notificación push */}
      {modalNotif && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md">

            {/* Header */}
            <div className="p-6 border-b">
              <h2 className="text-lg font-bold text-gray-900">Enviar Notificación</h2>
              <p className="text-sm text-gray-500 mt-1">
                Para: <span className="font-medium text-gray-700">{modalNotif.nombre}</span>
                {' '}· @{modalNotif.username}
              </p>
            </div>

            {/* Form */}
            <div className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Título
                </label>
                <input
                  type="text"
                  maxLength={100}
                  placeholder="Ej: Información importante"
                  value={notifForm.titulo}
                  onChange={e => setNotifForm(f => ({ ...f, titulo: e.target.value }))}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Mensaje
                </label>
                <textarea
                  maxLength={500}
                  rows={4}
                  placeholder="Escribe el mensaje que recibirá el usuario..."
                  value={notifForm.cuerpo}
                  onChange={e => setNotifForm(f => ({ ...f, cuerpo: e.target.value }))}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none resize-none"
                />
                <p className="text-xs text-gray-400 mt-1 text-right">
                  {notifForm.cuerpo.length}/500
                </p>
              </div>
            </div>

            {/* Footer */}
            <div className="p-6 border-t flex gap-3 justify-end">
              <button
                onClick={() => {
                  setModalNotif(null);
                  setNotifForm({ titulo: '', cuerpo: '' });
                }}
                className="px-4 py-2 text-sm text-gray-600 hover:text-gray-900"
              >
                Cancelar
              </button>
              <button
                onClick={handleNotificar}
                disabled={enviando || !notifForm.titulo.trim() || !notifForm.cuerpo.trim()}
                className="px-5 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-lg disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                {enviando ? 'Enviando...' : '🔔 Enviar notificación'}
              </button>
            </div>

          </div>
        </div>
      )}
    </div>
  );
};

export default Usuarios;
