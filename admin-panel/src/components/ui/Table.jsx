export const Table = ({ columns, data, loading, emptyMessage = 'No se encontraron datos' }) => {
  return (
    <div className="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-left">
          <thead className="bg-gray-50 border-b border-gray-100">
            <tr>
              {columns.map((col) => (
                <th key={col.key} className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                  {col.label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-50">
            {loading ? (
              [...Array(5)].map((_, i) => (
                <tr key={i} className="animate-pulse">
                  {columns.map((col) => (
                    <td key={col.key} className="px-6 py-4">
                      <div className="h-4 bg-gray-100 rounded w-full"></div>
                    </td>
                  ))}
                </tr>
              ))
            ) : !Array.isArray(data) || data.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className="px-6 py-10 text-center text-gray-500">
                  {!data ? 'Cargando...' : emptyMessage}
                </td>
              </tr>
            ) : (
              data.map((item, i) => (
                <tr key={item.id || i} className="hover:bg-gray-50 transition-colors">
                  {columns.map((col) => (
                    <td key={col.key} className="px-6 py-4 text-sm text-gray-700">
                      {col.render ? col.render(item) : item[col.key]}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
