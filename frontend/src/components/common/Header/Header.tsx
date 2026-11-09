import React from 'react';
import { Kanban, ShieldCheck, Sun, Moon } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/axios';
import { ENDPOINTS } from '@/services/api/endpoints';
import { HealthResponse } from '@/types/api';
import { useLocation, useNavigate } from 'react-router-dom';

interface HeaderProps {
  isDark: boolean;
  onToggleTheme: () => void;
}

export const Header: React.FC<HeaderProps> = ({ isDark, onToggleTheme }) => {
  const navigate = useNavigate();
  const location = useLocation();

  // Health Query to verify backend connection
  const { data: health, isLoading } = useQuery<HealthResponse>({
    queryKey: ['backend-health'],
    queryFn: async () => {
      const res = await apiClient.get<HealthResponse>(ENDPOINTS.HEALTH);
      return res.data;
    },
    retry: 1,
    refetchInterval: 10000,
  });

  return (
    <header className="border-b border-slate-200 dark:border-slate-800/80 bg-white/80 dark:bg-slate-900/60 backdrop-blur sticky top-0 z-50 px-6 py-3 flex items-center justify-between">
      <div className="flex items-center gap-4">
        {/* Logo */}
        <div className="flex items-center gap-2.5 cursor-pointer" onClick={() => navigate('/')}>
          <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-brand-600 to-indigo-500 flex items-center justify-center shadow-lg shadow-brand-500/20 text-white font-bold text-sm">
            NT
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-sm tracking-tight text-slate-900 dark:text-white">NexTrack</span>
              <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-brand-500/10 text-brand-600 dark:text-brand-400 border border-brand-500/20 font-semibold">
                Phase 5 Kanban
              </span>
            </div>
          </div>
        </div>

        <div className="h-4 w-px bg-slate-200 dark:bg-slate-800 hidden sm:block" />

        {/* Project Breadcrumb */}
        <div className="hidden sm:flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
          <span className="hover:text-slate-900 dark:hover:text-white cursor-pointer font-medium">Acme Engineering</span>
          <span>/</span>
          <span className="text-slate-900 dark:text-slate-200 font-semibold flex items-center gap-1.5">
            <span className="font-mono text-brand-600 dark:text-brand-400 bg-brand-500/10 px-1.5 py-0.5 rounded">NT</span>
            <span>NexTrack Core</span>
          </span>
        </div>

        <div className="h-4 w-px bg-slate-200 dark:bg-slate-800 hidden md:block" />

        {/* Navigation Tabs */}
        <nav className="flex items-center gap-1 text-xs">
          <button
            onClick={() => navigate('/')}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-medium transition-colors ${
              location.pathname === '/' || location.pathname.startsWith('/board')
                ? 'bg-brand-500/10 text-brand-600 dark:text-brand-400 font-bold border border-brand-500/20'
                : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
            }`}
          >
            <Kanban className="w-3.5 h-3.5" />
            <span>Board</span>
            <span className="text-[9px] font-mono opacity-50 ml-0.5 hidden lg:inline">1</span>
          </button>

          <button
            onClick={() => navigate('/dashboard')}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-medium transition-colors ${
              location.pathname === '/dashboard'
                ? 'bg-brand-500/10 text-brand-600 dark:text-brand-400 font-bold border border-brand-500/20'
                : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
            }`}
          >
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>Architecture & Status</span>
            <span className="text-[9px] font-mono opacity-50 ml-0.5 hidden lg:inline">2</span>
          </button>
        </nav>
      </div>

      {/* Right Header Status & Controls */}
      <div className="flex items-center gap-3">
        {/* Backend Status Badge */}
        <div className="flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium border bg-slate-100 dark:bg-slate-900/80 border-slate-200 dark:border-slate-800">
          <span 
            className={`w-2 h-2 rounded-full ${
              isLoading 
                ? 'bg-amber-400 animate-pulse' 
                : (health?.status === 'UP' ? 'bg-emerald-400 shadow-sm shadow-emerald-400/50' : 'bg-rose-400')
            }`} 
          />
          <span className="text-slate-600 dark:text-slate-300 font-mono text-[11px]">
            {isLoading ? 'Checking...' : (health?.status === 'UP' ? 'LIVE' : 'DEMO MODE')}
          </span>
        </div>

        {/* Theme Toggle Button */}
        <button
          onClick={onToggleTheme}
          className="p-2 rounded-lg border border-slate-200 dark:border-slate-800 text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-800/60 transition-colors"
          title="Toggle Theme (Cmd+T)"
        >
          {isDark ? <Sun className="w-4 h-4 text-amber-400" /> : <Moon className="w-4 h-4 text-slate-700" />}
        </button>
      </div>
    </header>
  );
};
