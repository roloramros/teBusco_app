export const Header = ({ onMenuClick, title }) => (
  <header className="bg-white border-b border-gray-200 h-16 flex items-center justify-between px-6 lg:px-8">
    <div className="flex items-center gap-4">
      <button onClick={onMenuClick} className="lg:hidden text-gray-500 hover:text-gray-700">
        ☰
      </button>
      <h2 className="text-xl font-semibold text-gray-800">{title}</h2>
    </div>
  </header>
);
