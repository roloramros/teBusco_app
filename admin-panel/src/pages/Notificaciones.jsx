import { useState } from 'react';
import { broadcastNotification } from '../api/admin';
import { Spinner } from '../components/ui/Spinner';
import toast from 'react-hot-toast';

const Notificaciones = () => {
  const [form, setForm] = useState({ titulo: '', cuerpo: '', tipo_usuario: '' });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!confirm('¿Confirmas el envío de esta notificación masiva?')) return;
    setLoading(true);
    try {
      const res = await broadcastNotification(form);
      toast.success(`Enviadas: ${res.enviadas}, Fallidas: ${res.fallidas}`);
      setForm({ titulo: '', cuerpo: '', tipo_usuario: '' });
    } catch (err) {
      toast.error('Error al enviar notificación');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
      <form onSubmit={handleSubmit} className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100 space-y-6">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Destinatarios</label>
          <select 
            className="w-full px-4 py-2 border border-gray-200 rounded-lg outline-none focus:ring-2 focus:ring-brand-500"
            value={form.tipo_usuario}
            onChange={(e) => setForm(prev => ({ ...prev, tipo_usuario: e.target.value }))}
          >
            <option value="">Todos los usuarios</option>
            <option value="pasajero">Solo pasajeros</option>
            <option value="chofer">Solo choferes</option>
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Título</label>
          <input 
            type="text" 
            required 
            maxLength={60}
            className="w-full px-4 py-2 border border-gray-200 rounded-lg outline-none focus:ring-2 focus:ring-brand-500"
            value={form.titulo}
            onChange={(e) => setForm(prev => ({ ...prev, titulo: e.target.value }))}
          />
          <p className="text-right text-[10px] text-gray-400 mt-1">{form.titulo.length}/60</p>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Mensaje</label>
          <textarea 
            required 
            rows={4}
            maxLength={300}
            className="w-full px-4 py-2 border border-gray-200 rounded-lg outline-none focus:ring-2 focus:ring-brand-500 resize-none"
            value={form.cuerpo}
            onChange={(e) => setForm(prev => ({ ...prev, cuerpo: e.target.value }))}
          />
          <p className="text-right text-[10px] text-gray-400 mt-1">{form.cuerpo.length}/300</p>
        </div>
        <button 
          disabled={loading || !form.titulo || !form.cuerpo}
          className="w-full bg-brand-500 hover:bg-brand-600 text-white font-bold py-3 rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
        >
          {loading ? <Spinner size="sm" className="border-white" /> : '🚀 Enviar Notificación'}
        </button>
      </form>

      <div className="space-y-4">
        <h3 className="text-sm font-bold text-gray-500 uppercase tracking-widest">Vista previa</h3>
        <div className="bg-gray-100 p-6 rounded-2xl flex items-center justify-center min-h-[300px]">
          <div className="bg-[#1c1c1e] text-white p-4 rounded-2xl w-full max-w-sm shadow-2xl">
            <div className="flex items-center gap-2 mb-2">
              <span className="text-xs bg-gray-700 p-1 rounded">🚕</span>
              <span className="text-[10px] font-bold text-gray-400 uppercase">Te Busco • ahora</span>
            </div>
            <p className="font-bold text-sm">{form.titulo || 'Título de notificación'}</p>
            <p className="text-sm text-gray-300 mt-0.5 line-clamp-2">{form.cuerpo || 'Este es un ejemplo de cómo se verá el mensaje en el dispositivo del usuario.'}</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Notificaciones;
