import { useState, useEffect } from 'react';
import { getStats } from '../api/admin';
import { StatCard } from '../components/ui/StatCard';
import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from 'recharts';
import toast from 'react-hot-toast';

const Dashboard = () => {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getStats()
      .then(setStats)
      .catch(err => toast.error('Error al cargar estadísticas'))
      .finally(() => setLoading(false));
  }, []);

  const chartData = [
    { name: 'Activos', value: stats?.viajes_activos || 0, color: '#3b82f6' },
    { name: 'Completados', value: stats?.viajes_completados || 0, color: '#10b981' },
    { name: 'Cancelados', value: stats?.viajes_cancelados || 0, color: '#ef4444' },
  ];

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard title="Usuarios Totales" value={stats?.total_usuarios} icon="👥" loading={loading} color="blue" />
        <StatCard title="Choferes" value={stats?.total_choferes} icon="🚗" loading={loading} color="purple" />
        <StatCard title="Viajes Completados" value={stats?.viajes_completados} icon="✅" loading={loading} color="green" />
        <StatCard title="Viajes Activos" value={stats?.viajes_activos} icon="🔥" loading={loading} color="yellow" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-white p-6 rounded-xl border border-gray-100 shadow-sm h-80">
          <h3 className="text-lg font-bold mb-4">Distribución de Viajes</h3>
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie data={chartData} innerRadius={60} outerRadius={80} paddingAngle={5} dataKey="value">
                {chartData.map((entry, index) => <Cell key={`cell-${index}`} fill={entry.color} />)}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        </div>
        
        <div className="bg-white p-6 rounded-xl border border-gray-100 shadow-sm">
          <h3 className="text-lg font-bold mb-4">Resumen</h3>
          <div className="space-y-4">
            <div className="flex justify-between items-center pb-2 border-b border-gray-50">
              <span className="text-gray-500">Pendientes de Aprobación</span>
              <span className={`font-bold ${stats?.choferes_pendientes > 0 ? 'text-red-500' : 'text-gray-900'}`}>{stats?.choferes_pendientes}</span>
            </div>
            <div className="flex justify-between items-center pb-2 border-b border-gray-50">
              <span className="text-gray-500">Valoración Promedio</span>
              <span className="font-bold text-yellow-500">⭐ {stats?.valoracion_promedio}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
