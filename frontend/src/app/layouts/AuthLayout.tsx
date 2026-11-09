import React from 'react';
import { Outlet } from 'react-router-dom';
import { useTheme } from '../providers/ThemeProvider';
import { Sun, Moon } from 'lucide-react';

export const AuthLayout: React.FC = () => {
  const { isDark, toggleTheme } = useTheme();

  return (
    <div className={`min-h-screen flex flex-col items-center justify-center p-6 ${isDark ? 'dark bg-slate-950 text-slate-100' : 'bg-slate-50 text-slate-900'} transition-colors duration-200 relative`}>
      <button
        onClick={toggleTheme}
        className="absolute top-6 right-6 p-2 rounded-lg border border-slate-200 dark:border-slate-800 text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-100"
      >
        {isDark ? <Sun className="w-4 h-4 text-amber-400" /> : <Moon className="w-4 h-4 text-slate-700" />}
      </button>

      <div className="w-full max-w-md flex flex-col items-center">
        <div className="flex items-center gap-3 mb-8">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-brand-600 to-indigo-500 flex items-center justify-center shadow-lg shadow-brand-500/20 text-white font-bold text-lg">
            NT
          </div>
          <span className="font-bold text-xl tracking-tight">NexTrack</span>
        </div>
        <Outlet />
      </div>
    </div>
  );
};
