import { useState, useEffect } from 'react';
import { getChoferes, getChoferById, aprobarChofer, getProvincias, actualizarCuotaMasiva, getStats, getLicenciasStats, registrarPago, cambiarEstadoLicencia } from '../api/admin';
import { Table } from '../components/ui/Table';
import { Badge } from '../components/ui/Badge';
import { Pagination } from '../components/ui/Pagination';
import { formatRating, formatDateShort } from '../utils/formatters';
import toast from 'react-hot-toast';

// Mapeo de colores para estados de licencia
const ESTADO_COLORS = {
  TRIAL_ACTIVO:    'bg-blue-100 text-blue-800',
  ACTIVO:          'bg-green-100 text-green-800',
  SUSPENDIDO:      'bg-yellow-100 text-yellow-800',
  BLOQUEADO:       'bg-gray-200 text-gray-700',
};

const ESTADO_LABELS = {
  TRIAL_ACTIVO:    'Trial activo',
  ACTIVO:          'Activo',
  SUSPENDIDO:      'Suspendido',
  BLOQUEADO:       'Bloqueado',
};

// — Componente de badge para licencias —
const LicenciaBadge = ({ estado }) => (
  <span className={`px-2 py-1 rounded-full text-xs font-semibold ${ESTADO_COLORS[estado] || 'bg-gray-100 text-gray-800'}`}>
    {ESTADO_LABELS[estado] || estado}
  </span>
);

// — Modal: Registrar pago / Depósito —
const ModalPago = ({ chofer, onClose, onSuccess }) => {
  const [form, setForm] = useState({ 
    monto: '', 
    monto_mensual: chofer.monto_mensual || '', 
    notas: '' 
  });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    if (form.monto === '' || parseFloat(form.monto) < 0) {
      toast.error('Ingresa un monto de depósito válido');
      return;
    }
    setLoading(true);
    try {
      await registrarPago(chofer.id, {
        monto: parseFloat(form.monto),
        monto_mensual: form.monto_mensual ? parseFloat(form.monto_mensual) : undefined,
        notas: form.notas || undefined
      });
      toast.success('Depósito registrado correctamente');
      onSuccess();
      onClose();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Error al registrar depósito');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md">
        <div className="p-6 border-b">
          <h2 className="text-lg font-bold text-gray-900">Gestionar Saldo y Cuota</h2>
          <p className="text-sm text-gray-500 mt-1">{chofer.nombre} · @{chofer.username}</p>
        </div>
        <div className="p-6 space-y-4">
          <div className="bg-blue-50 p-3 rounded-lg flex justify-between items-center">
            <span className="text-sm text-blue-800">Saldo actual:</span>
            <span className="font-bold text-blue-900">${parseFloat(chofer.saldo_fondo || 0).toFixed(2)}</span>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Monto a depositar</label>
            <input
              type="number"
              min="0"
              step="0.01"
              placeholder="0.00"
              value={form.monto}
              onChange={e => setForm(f => ({ ...f, monto: e.target.value }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
            <p className="text-xs text-gray-400 mt-1">Este dinero se sumará al saldo actual.</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Cuota Mensual ($)</label>
            <input
              type="number"
              min="0"
              step="1"
              placeholder="500"
              value={form.monto_mensual}
              onChange={e => setForm(f => ({ ...f, monto_mensual: e.target.value }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
            <p className="text-xs text-gray-400 mt-1">Lo que se descontará automáticamente el día del vencimiento.</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Notas (opcional)</label>
            <textarea
              placeholder="Referencia del pago, etc."
              value={form.notas}
              onChange={e => setForm(f => ({ ...f, notas: e.target.value }))}
              rows={2}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 resize-none"
            />
          </div>
        </div>
        <div className="p-6 border-t flex gap-3 justify-end">
          <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-900">
            Cancelar
          </button>
          <button
            onClick={handleSubmit}
            disabled={loading}
            className="px-5 py-2 bg-green-600 hover:bg-green-700 text-white text-sm font-medium rounded-lg disabled:opacity-50"
          >
            {loading ? 'Guardando...' : 'Actualizar Fondo'}
          </button>
        </div>
      </div>
    </div>
  );
};

// — Modal: Cambiar estado manualmente —
const ModalEstado = ({ chofer, onClose, onSuccess }) => {
  const [estado, setEstado] = useState('');
  const [notas, setNotas] = useState('');
  const [loading, setLoading] = useState(false);

  const estados = [
    { value: 'TRIAL_ACTIVO',   label: '🔵 Trial activo (extender 45 días)' },
    { value: 'ACTIVO',         label: '✅ Activar manualmente' },
    { value: 'SUSPENDIDO',     label: '⚠️ Suspender' },
    { value: 'BLOQUEADO',      label: '🚫 Bloquear' },
  ];

  const handleSubmit = async () => {
    if (!estado) { toast.error('Selecciona un estado'); return; }
    if (!confirm(`¿Confirmas cambiar la licencia de ${chofer.nombre} a "${ESTADO_LABELS[estado]}"?`)) return;
    setLoading(true);
    try {
      await cambiarEstadoLicencia(chofer.id, { estado, notas: notas || undefined });
      toast.success('Estado de licencia actualizado');
      onSuccess();
      onClose();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Error al cambiar estado');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md">
        <div className="p-6 border-b">
          <h2 className="text-lg font-bold text-gray-900">Cambiar Estado de Licencia</h2>
          <p className="text-sm text-gray-500 mt-1">{chofer.nombre} · @{chofer.username}</p>
        </div>
        <div className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Nuevo estado</label>
            <select
              value={estado}
              onChange={e => setEstado(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
            >
              <option value="">Seleccionar...</option>
              {estados.map(e => (
                <option key={e.value} value={e.value}>{e.label}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Motivo / Notas</label>
            <textarea
              placeholder="Razón del cambio de estado..."
              value={notas}
              onChange={e => setNotas(e.target.value)}
              rows={2}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 resize-none"
            />
          </div>
        </div>
        <div className="p-6 border-t flex gap-3 justify-end">
          <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-900">
            Cancelar
          </button>
          <button
            onClick={handleSubmit}
            disabled={loading}
            className="px-5 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-lg disabled:opacity-50"
          >
            {loading ? 'Guardando...' : 'Aplicar cambio'}
          </button>
        </div>
      </div>
    </div>
  );
};

// — Modal: Actualizar Cuota Masiva —
const ModalCuotaMasiva = ({ onClose, onSuccess }) => {
  const [monto, setMonto] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    if (!monto || parseFloat(monto) < 0) {
      toast.error('Ingresa un monto válido');
      return;
    }
    if (!confirm(`¿Estás seguro de que quieres cambiar la cuota a $${monto} para TODOS los choferes?`)) return;
    
    setLoading(true);
    try {
      const res = await actualizarCuotaMasiva(parseFloat(monto));
      toast.success(res.message || 'Cuotas actualizadas correctamente');
      onSuccess();
      onClose();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Error al actualizar cuotas');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md">
        <div className="p-6 border-b text-center">
          <div className="w-12 h-12 bg-orange-100 text-orange-600 rounded-full flex items-center justify-center mx-auto mb-3">
            <span className="text-2xl">📢</span>
          </div>
          <h2 className="text-lg font-bold text-gray-900">Actualización Masiva</h2>
          <p className="text-sm text-gray-500 mt-1">Cambiar la cuota mensual de todos los choferes registrados.</p>
        </div>
        <div className="p-6 space-y-4">
          <div className="bg-orange-50 border border-orange-200 p-3 rounded-lg">
            <p className="text-xs text-orange-800">
              <strong>Nota:</strong> Este cambio afectará a todos los choferes en su próxima renovación automática. No afecta los saldos actuales.
            </p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Nueva Cuota Mensual ($)</label>
            <input
              type="number"
              min="0"
              placeholder="Ej: 600"
              value={monto}
              onChange={e => setMonto(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-transparent"
            />
          </div>
        </div>
        <div className="p-6 border-t flex gap-3 justify-end">
          <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-900">
            Cancelar
          </button>
          <button
            onClick={handleSubmit}
            disabled={loading}
            className="px-5 py-2 bg-orange-600 hover:bg-orange-700 text-white text-sm font-medium rounded-lg disabled:opacity-50"
          >
            {loading ? 'Procesando...' : 'Actualizar a Todos'}
          </button>
        </div>
      </div>
    </div>
  );
};

// — Modal: Ver Vehículos —
const ModalVehiculos = ({ chofer, onClose }) => {
  const [vehiculos, setVehiculos] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (chofer) {
      setLoading(true);
      getChoferById(chofer.id)
        .then(res => {
          setVehiculos(res.vehiculos || []);
        })
        .catch(() => toast.error('Error al cargar vehículos'))
        .finally(() => setLoading(false));
    }
  }, [chofer]);

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-2xl overflow-hidden">
        <div className="p-6 border-b flex justify-between items-center bg-gray-50">
          <div>
            <h2 className="text-lg font-bold text-gray-900">Vehículos Registrados</h2>
            <p className="text-sm text-gray-500 mt-0.5">{chofer.nombre} · @{chofer.username}</p>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <span className="text-2xl">×</span>
          </button>
        </div>
        
        <div className="p-6 max-h-[70vh] overflow-y-auto">
          {loading ? (
            <div className="py-12 text-center text-gray-500">Cargando vehículos...</div>
          ) : vehiculos.length === 0 ? (
            <div className="py-12 text-center text-gray-500 bg-gray-50 rounded-xl border-2 border-dashed">
              Este chofer aún no ha registrado vehículos.
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {vehiculos.map(v => (
                <div key={v.id} className="border rounded-xl p-4 flex flex-col gap-3 hover:border-blue-200 transition-colors">
                  <div className="aspect-video w-full bg-gray-100 rounded-lg overflow-hidden relative">
                    {v.foto_url ? (
                      <img src={v.foto_url} alt={v.marca} className="w-full h-full object-cover" />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center text-gray-400">
                        🚗 No hay foto
                      </div>
                    )}
                    {!v.activo && (
                      <div className="absolute top-2 right-2">
                        <Badge status="inactivo">Eliminado</Badge>
                      </div>
                    )}
                  </div>
                  <div>
                    <div className="flex justify-between items-start">
                      <p className="font-bold text-gray-900">{v.marca}</p>
                      <span className="px-2 py-0.5 bg-gray-100 text-gray-600 rounded text-[10px] font-mono uppercase">{v.placa}</span>
                    </div>
                    <div className="mt-1 flex gap-2 flex-wrap">
                      <span className="text-xs text-gray-500 bg-gray-100 px-2 py-0.5 rounded uppercase">{v.tipo}</span>
                      <span className="text-xs text-gray-500 bg-gray-100 px-2 py-0.5 rounded">👥 {v.capacidad_pasajeros} plazas</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
        
        <div className="p-6 border-t bg-gray-50 flex justify-end">
          <button onClick={onClose} className="px-6 py-2 bg-white border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 shadow-sm">
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
};

const Choferes = () => {
  const [data, setData] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const ITEMS_PER_PAGE = 20;

  const [provincias, setProvincias] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState({ estado: '', pendiente: false, provincia_id: '', search: '' });
  const [modalMasivo, setModalMasivo] = useState(false);
  const [modalPago, setModalPago] = useState(null);    // chofer seleccionado
  const [modalEstado, setModalEstado] = useState(null); // chofer seleccionado
  const [modalVehiculos, setModalVehiculos] = useState(null); // chofer seleccionado

  const loadStats = () => {
    getLicenciasStats().then(setStats).catch(() => {});
  };

  const loadInitialData = async () => {
    try {
      const [provs, sGral] = await Promise.all([
        getProvincias(),
        getStats()
      ]);
      setProvincias(provs);
      // Las stats de licencias las cargamos con loadStats para unificar
      loadStats();
      setStats(prev => ({ ...prev, pendientes: sGral.choferes_pendientes }));
    } catch (err) {
      toast.error('Error al cargar datos iniciales');
    }
  };

  const loadChoferes = () => {
    setLoading(true);
    const params = { 
      estado: filter.estado || undefined,
      pendiente: filter.pendiente ? '1' : undefined,
      provincia_id: filter.provincia_id || undefined,
      search: filter.search || undefined,
      page: page,
      limit: ITEMS_PER_PAGE
    };
    getChoferes(params)
      .then(res => {
        setData(res.data);
        setTotal(res.total);
      })
      .catch(() => toast.error('Error al cargar choferes'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadInitialData();
  }, []);

  useEffect(() => {
    setPage(1); // Resetear si cambian los filtros
  }, [filter]);

  useEffect(() => {
    loadChoferes();
  }, [filter, page]);

  const handleSuccess = () => {
    loadChoferes();
    loadStats();
    // También refrescar stats generales por si cambió el conteo de pendientes
    getStats().then(sGral => {
      setStats(prev => ({ ...prev, pendientes: sGral.choferes_pendientes }));
    }).catch(() => {});
  };

  const handleAprobar = async (id) => {
    if (!confirm('¿Confirmas la aprobación de este chofer?')) return;
    try {
      await aprobarChofer(id);
      toast.success('Chofer aprobado con éxito');
      handleSuccess();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Error al aprobar');
    }
  };

  const columns = [
    { 
      key: 'chofer', 
      label: 'Chofer',
      render: (item) => (
        <div>
          <p className="font-medium text-gray-900">{item.nombre}</p>
          <p className="text-xs text-gray-500">@{item.username} · {item.telefono}</p>
          <p className="text-xs text-gray-400">{item.provincia}</p>
        </div>
      )
    },
    { 
      key: 'chofer_estado', 
      label: 'Estado',
      render: (item) => (
        <Badge status={item.verificado ? item.chofer_estado : 'pendiente'}>
          {item.verificado ? item.chofer_estado : 'Pendiente'}
        </Badge>
      )
    },
    {
      key: 'estado',
      label: 'Licencia',
      render: (item) => (
        <div className="space-y-1">
          <LicenciaBadge estado={item.estado} />
          {(item.estado === 'TRIAL_ACTIVO' || item.estado === 'ACTIVO') && (
            <p className="text-xs text-gray-500">
              {item.dias_restantes === 0 ? 'Vence hoy' : `${item.dias_restantes} días restantes`}
            </p>
          )}
        </div>
      )
    },
    {
      key: 'fondo',
      label: 'Fondo / Cuota',
      render: (item) => (
        <div className="text-xs">
          <p className="font-bold text-blue-700">Saldo: ${parseFloat(item.saldo_fondo || 0).toFixed(2)}</p>
          <p className="text-gray-500 font-medium">Cuota: ${parseFloat(item.monto_mensual || 0).toFixed(2)}/mes</p>
        </div>
      )
    },
    {
      key: 'fechas',
      label: 'Fechas',
      render: (item) => (
        <div className="text-xs text-gray-500 space-y-0.5">
          <p>Trial: {formatDateShort(item.trial_inicio)} → {formatDateShort(item.trial_fin)}</p>
          {item.suscripcion_fin && (
            <p>Suscr.: hasta {formatDateShort(item.suscripcion_fin)}</p>
          )}
          {item.ultimo_pago && (
            <p className="text-green-600">Último pago: {formatDateShort(item.ultimo_pago)}</p>
          )}
        </div>
      )
    },
    { key: 'calificacion_promedio', label: 'Calificación', render: (item) => formatRating(item.calificacion_promedio) },
    {
      key: 'acciones',
      label: 'Acciones',
      render: (item) => (
        <div className="flex gap-2 flex-wrap">
          {!item.verificado ? (
            <button 
              onClick={() => handleAprobar(item.id)}
              className="px-3 py-1 bg-green-500 text-white text-xs font-medium rounded-lg hover:bg-green-600 transition-colors"
            >
              Aprobar
            </button>
          ) : (
            <>
              <button
                onClick={() => setModalVehiculos(item)}
                className="px-3 py-1 bg-gray-600 hover:bg-gray-700 text-white text-xs font-medium rounded-lg"
                title="Ver vehículos"
              >
                🚗 Autos
              </button>
              <button
                onClick={() => setModalPago(item)}
                className="px-3 py-1 bg-green-600 hover:bg-green-700 text-white text-xs font-medium rounded-lg"
              >
                💰 Pago
              </button>
              <button
                onClick={() => setModalEstado(item)}
                className="px-3 py-1 bg-blue-600 hover:bg-blue-700 text-white text-xs font-medium rounded-lg"
              >
                ✏️ Estado
              </button>
            </>
          )}
        </div>
      )
    }
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2">
        <h1 className="text-2xl font-bold text-gray-900">Gestión de Choferes</h1>
        <p className="text-sm text-gray-500">Administra choferes, verificaciones y estados de licencias</p>
      </div>

      {/* Stats */}
      {stats && (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
          {[
            { label: 'Pendientes',      value: stats.pendientes,      color: 'bg-orange-50 border-orange-200 text-orange-800' },
            { label: 'Trial activo',     value: stats.trial_activo,    color: 'bg-blue-50 border-blue-200 text-blue-800' },
            { label: 'Activos',          value: stats.activo,          color: 'bg-green-50 border-green-200 text-green-800' },
            { label: 'Suspendidos',      value: stats.suspendido,      color: 'bg-yellow-50 border-yellow-200 text-yellow-800' },
            { label: 'Bloqueados',       value: stats.bloqueado,       color: 'bg-gray-50 border-gray-200 text-gray-700' },
          ].map(s => (
            <div key={s.label} className={`border rounded-xl p-4 ${s.color}`}>
              <p className="text-2xl font-bold">{s.value ?? 0}</p>
              <p className="text-xs font-medium mt-1">{s.label}</p>
            </div>
          ))}
        </div>
      )}

      <div className="flex flex-wrap gap-4 items-center justify-between bg-white p-4 rounded-xl border border-gray-100 shadow-sm">
        <div className="flex flex-wrap gap-4 items-center">
          <input 
            type="text"
            placeholder="Buscar por usuario, nombre..."
            className="px-4 py-2 border border-gray-200 rounded-lg outline-none focus:ring-2 focus:ring-brand-500 text-sm w-64"
            value={filter.search}
            onChange={(e) => setFilter(prev => ({ ...prev, search: e.target.value }))}
          />
          <select 
            className="px-4 py-2 border border-gray-200 rounded-lg outline-none focus:ring-2 focus:ring-brand-500 text-sm"
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

        <button
          onClick={() => setModalMasivo(true)}
          className="px-4 py-2 bg-orange-100 text-orange-700 hover:bg-orange-200 text-sm font-semibold rounded-lg flex items-center gap-2"
        >
          <span>📢</span> Actualizar Cuotas
        </button>
      </div>

      <Table columns={columns} data={data} loading={loading} />

      <Pagination 
        currentPage={page} 
        totalItems={total} 
        itemsPerPage={ITEMS_PER_PAGE} 
        onPageChange={setPage} 
      />

      {modalMasivo && (
        <ModalCuotaMasiva
          onClose={() => setModalMasivo(false)}
          onSuccess={handleSuccess}
        />
      )}

      {modalPago && (
        <ModalPago
          chofer={modalPago}
          onClose={() => setModalPago(null)}
          onSuccess={handleSuccess}
        />
      )}

      {modalEstado && (
        <ModalEstado
          chofer={modalEstado}
          onClose={() => setModalEstado(null)}
          onSuccess={handleSuccess}
        />
      )}

      {modalVehiculos && (
        <ModalVehiculos
          chofer={modalVehiculos}
          onClose={() => setModalVehiculos(null)}
        />
      )}
    </div>
  );
};

export default Choferes;
