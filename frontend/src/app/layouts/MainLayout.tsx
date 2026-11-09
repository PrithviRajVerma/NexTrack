import React from 'react';
import { Outlet } from 'react-router-dom';
import { Header } from '@/components/common/Header';
import { Sidebar } from '@/components/common/Sidebar';
import { useTheme } from '../providers/ThemeProvider';

export const MainLayout: React.FC = () => {
  const { isDark, toggleTheme } = useTheme();

  return (
    <div className={`min-h-screen flex flex-col ${isDark ? 'dark bg-slate-950 text-slate-100' : 'bg-slate-50 text-slate-900'} transition-colors duration-200`}>
      <Header isDark={isDark} onToggleTheme={toggleTheme} />
      <div className="flex-1 flex min-h-0">
        <Sidebar />
        <div className="flex-1 flex flex-col min-h-0 overflow-y-auto">
          <Outlet />
        </div>
      </div>
    </div>
  );
};
