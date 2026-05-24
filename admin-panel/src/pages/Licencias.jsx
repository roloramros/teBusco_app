import { useState, useEffect } from 'react';
import { getLicencias, getLicenciasStats, registrarPago, cambiarEstadoLicencia, getProvincias } from '../api/admin';
import { Table } from '../components/ui/Table';
import { formatDateShort } from '../utils/formatters';
import toast from 'react-hot-toast';

// Mapeo de colores para estados de licencia
const ESTADO_COLORS = {
  TRIAL_ACTIVO:    'bg-blue-100 text-blue-800',
  TRIAL_EXPIRADO:  'bg-red-100 text-red-700',
  ACTIVO:          'bg-green-100 text-green-800',
  SUSPENDIDO:      'bg-yellow-100 text-yellow-800',
  BLOQUEADO:       'bg-gray-200 text-gray-700',
};

const ESTADO_LABELS = {
  TRIAL_ACTIVO:    'Trial activo',
  TRIAL_EXPIRADO:  'Trial expirado',
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

// — Modal: Registrar pago —
const ModalPago = ({ chofer, onClose, onSuccess }) => {
  const [form, setForm] = useState({ monto: '', meses: 1, notas: '' });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    if (!form.monto || parseFloat(form.monto) <= 0) {
      toast.error('Ingresa un monto válido');
      return;
    }
    setLoading(true);
    try {
      await registrarPago(chofer.chofer_id, {
        monto: parseFloat(form.monto),
        meses: parseInt(form.meses),
        notas: form.notas || undefined
      });
      toast.success('Pago registrado correctamente');
      onSuccess();
      onClose();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Error al registrar pago');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md">
        <div className="p-6 border-b">
          <h2 className="text-lg font-bold text-gray-900">Registrar Pago</h2>
          <p className="text-sm text-gray-500 mt-1">{chofer.nombre} · @{chofer.username}</p>
        </div>
        <div className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Monto cobrado</label>
            <input
              type="number"
              min="0"
              step="0.01"
              placeholder="0.00"
              value={form.monto}
              onChange={e => setForm(f => ({ ...f, monto: e.target.value }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Meses a activar</label>
            <select
              value={form.meses}
              onChange={e => setForm(f => ({ ...f, meses: e.target.value }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
            >
              {[1,2,3,6,12].map(m => (
                <option key={m} value={m}>{m} {m === 1 ? 'mes' : 'meses'}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Notas (opcional)</label>
            <textarea
              placeholder="Método de pago, referencia, etc."
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
            {loading ? 'Guardando...' : 'Confirmar pago'}
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
      await cambiarEstadoLicencia(chofer.chofer_id, { estado, notas: notas || undefined });
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

// — Página principal —
const Licencias = () => {
  const [data, setData] = useState([]);
  const [stats, setStats] = useState(null);
  const [provincias, setProvincias] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState({ estado: '', provincia_id: '', alerta: '' });
  const [modalPago, setModalPago] = useState(null);    // chofer seleccionado
  const [modalEstado, setModalEstado] = useState(null); // chofer seleccionado

  const loadStats = () => {
    getLicenciasStats().then(setStats).catch(() => {});
  };

  const loadData = () => {
    setLoading(true);
    const params = {
      estado:      filter.estado || undefined,
      provincia_id: filter.provincia_id || undefined,
      alerta:      filter.alerta || undefined,
    };
    getLicencias(params)
      .then(setData)
      .catch(() => toast.error('Error al cargar licencias'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadStats();
    getProvincias().then(setProvincias).catch(() => {});
  }, []);

  useEffect(() => {
    loadData();
  }, [filter]);

  const handleSuccess = () => {
    loadData();
    loadStats();
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
    {
      key: 'notas',
      label: 'Notas',
      render: (item) => (
        <p className="text-xs text-gray-500 max-w-xs truncate">{item.notas || '—'}</p>
      )
    },
    {
      key: 'acciones',
      label: 'Acciones',
      render: (item) => (
        <div className="flex gap-2 flex-wrap">
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
        </div>
      )
    }
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Licencias de Uso</h1>
        <p className="text-sm text-gray-500 mt-1">Gestión de suscripciones y períodos de prueba de choferes</p>
      </div>

      {/* Stats */}
      {stats && (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
          {[
            { label: 'Trial activo',     value: stats.trial_activo,    color: 'bg-blue-50 border-blue-200 text-blue-800' },
            { label: 'Trial expirado',   value: stats.trial_expirado,  color: 'bg-red-50 border-red-200 text-red-800' },
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

      {/* Alerta de vencimientos próximos */}
      {stats && (parseInt(stats.trial_por_vencer) + parseInt(stats.suscripcion_por_vencer)) > 0 && (
        <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 flex items-center gap-3">
          <span className="text-2xl">⚠️</span>
          <div>
            <p className="font-semibold text-amber-800">Licencias próximas a vencer</p>
            <p className="text-sm text-amber-700">
              {stats.trial_por_vencer} trials y {stats.suscripcion_por_vencer} suscripciones vencen en los próximos 7 días.
              <button
                onClick={() => setFilter(f => ({ ...f, alerta: '1' }))}
                className="ml-2 underline font-medium"
              >
                Ver listado
              </button>
            </p>
          </div>
        </div>
      )}

      {/* Filtros */}
      <div className="flex flex-wrap gap-3">
        <select
          value={filter.estado}
          onChange={e => setFilter(f => ({ ...f, estado: e.target.value, alerta: '' }))}
          className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
        >
          <option value="">Todos los estados</option>
          <option value="TRIAL_ACTIVO">Trial activo</option>
          <option value="TRIAL_EXPIRADO">Trial expirado</option>
          <option value="ACTIVO">Activo</option>
          <option value="SUSPENDIDO">Suspendido</option>
          <option value="BLOQUEADO">Bloqueado</option>
        </select>

        <select
          value={filter.provincia_id}
          onChange={e => setFilter(f => ({ ...f, provincia_id: e.target.value }))}
          className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
        >
          <option value="">Todas las provincias</option>
          {provincias.map(p => (
            <option key={p.id} value={p.id}>{p.nombre}</option>
          ))}
        </select>

        <button
          onClick={() => setFilter({ estado: '', provincia_id: '', alerta: '' })}
          className="px-4 py-2 text-sm text-gray-600 hover:text-gray-900 border border-gray-300 rounded-lg"
        >
          Limpiar filtros
        </button>
      </div>

      {/* Tabla */}
      <Table columns={columns} data={data} loading={loading} emptyMessage="No hay licencias que coincidan con los filtros" />

      {/* Modales */}
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
    </div>
  );
};

export default Licencias;
