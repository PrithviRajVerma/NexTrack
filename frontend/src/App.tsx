import React, { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { 
  Sun, 
  Moon, 
  Layers, 
  Kanban, 
  Server, 
  Database, 
  ShieldCheck, 
  CheckCircle2, 
  Activity
} from 'lucide-react';
import { KanbanBoard } from './features/board/KanbanBoard';

interface HealthResponse {
  status: string;
}

export const App: React.FC = () => {
  const [isDark, setIsDark] = useState(true);
  const [activeTab, setActiveTab] = useState<'board' | 'architecture'>('board');

  // Toggle Theme
  const toggleTheme = () => {
    setIsDark(!isDark);
    document.documentElement.classList.toggle('dark');
  };

  // Keyboard shortcut Cmd+T / Ctrl+T to toggle theme, 1/2 to switch tabs
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) {
        return;
      }
      if ((e.metaKey || e.ctrlKey) && (e.key === 't' || e.key === 'T')) {
        e.preventDefault();
        toggleTheme();
      }
      if (e.key === '1') {
        setActiveTab('board');
      } else if (e.key === '2') {
        setActiveTab('architecture');
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isDark]);

  // Health Query to verify backend connection
  const { data: health, isLoading } = useQuery<HealthResponse>({
    queryKey: ['backend-health'],
    queryFn: async () => {
      const res = await fetch('/actuator/health');
      if (!res.ok) throw new Error('Backend unreachable');
      return res.json();
    },
    retry: 1,
    refetchInterval: 10000,
  });

  return (
    <div className={`min-h-screen flex flex-col ${isDark ? 'dark bg-slate-950 text-slate-100' : 'bg-slate-50 text-slate-900'} transition-colors duration-200`}>
      {/* Top Application Header */}
      <header className="border-b border-slate-200 dark:border-slate-800/80 bg-white/80 dark:bg-slate-900/60 backdrop-blur sticky top-0 z-50 px-6 py-3 flex items-center justify-between">
        <div className="flex items-center gap-4">
          {/* Logo */}
          <div className="flex items-center gap-2.5 cursor-pointer" onClick={() => setActiveTab('board')}>
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
              onClick={() => setActiveTab('board')}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-medium transition-colors ${
                activeTab === 'board'
                  ? 'bg-brand-500/10 text-brand-600 dark:text-brand-400 font-bold border border-brand-500/20'
                  : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
              }`}
            >
              <Kanban className="w-3.5 h-3.5" />
              <span>Board</span>
              <span className="text-[9px] font-mono opacity-50 ml-0.5 hidden lg:inline">1</span>
            </button>

            <button
              onClick={() => setActiveTab('architecture')}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-medium transition-colors ${
                activeTab === 'architecture'
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
            onClick={toggleTheme}
            className="p-2 rounded-lg border border-slate-200 dark:border-slate-800 text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-800/60 transition-colors"
            title="Toggle Theme (Cmd+T)"
          >
            {isDark ? <Sun className="w-4 h-4 text-amber-400" /> : <Moon className="w-4 h-4 text-slate-700" />}
          </button>
        </div>
      </header>

      {/* Main View Area */}
      {activeTab === 'board' ? (
        <KanbanBoard />
      ) : (
        <main className="max-w-6xl mx-auto px-6 py-12 flex-1">
          <div className="text-center max-w-2xl mx-auto mb-10">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-mono bg-brand-500/10 text-brand-600 dark:text-brand-400 border border-brand-500/20 mb-4">
              <Activity className="w-3.5 h-3.5" />
              Phase 5 Kanban Board View Projections Online
            </div>
            <h1 className="text-3xl font-extrabold tracking-tight sm:text-4xl bg-clip-text text-transparent bg-gradient-to-r from-slate-900 via-slate-700 to-slate-950 dark:from-slate-100 dark:via-slate-200 dark:to-slate-400">
              Modular Monolith Engineering Tracker
            </h1>
            <p className="mt-3 text-slate-600 dark:text-slate-400 text-sm sm:text-base">
              Interactive Kanban board with read-optimized projections (`v_board_cards`), drag-and-drop transitions, and optimistic conflict rollback.
            </p>
          </div>

          {/* Architecture Cards Grid */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-10">
            <div className="p-6 rounded-2xl border border-slate-200 dark:border-slate-800/80 bg-white dark:bg-slate-900/40 backdrop-blur shadow-sm">
              <div className="w-10 h-10 rounded-xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-500 mb-4">
                <Server className="w-5 h-5" />
              </div>
              <h3 className="font-bold text-base mb-1">Spring Boot 3.3.5 Backend</h3>
              <p className="text-slate-500 dark:text-slate-400 text-xs leading-relaxed">
                Java 21 LTS modular monolith partitioned with Spring Modulith, HikariCP connection pooling, and RFC 7807 Problem Details.
              </p>
            </div>

            <div className="p-6 rounded-2xl border border-slate-200 dark:border-slate-800/80 bg-white dark:bg-slate-900/40 backdrop-blur shadow-sm">
              <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-500 mb-4">
                <Database className="w-5 h-5" />
              </div>
              <h3 className="font-bold text-base mb-1">Database View Projections</h3>
              <p className="text-slate-500 dark:text-slate-400 text-xs leading-relaxed">
                PostgreSQL view `v_board_cards` eliminating N+1 queries by providing lateral subqueries for comments and VCS telemetry.
              </p>
            </div>

            <div className="p-6 rounded-2xl border border-slate-200 dark:border-slate-800/80 bg-white dark:bg-slate-900/40 backdrop-blur shadow-sm">
              <div className="w-10 h-10 rounded-xl bg-brand-500/10 border border-brand-500/20 flex items-center justify-center text-brand-500 mb-4">
                <Layers className="w-5 h-5" />
              </div>
              <h3 className="font-bold text-base mb-1">Optimistic UI & Concurrency</h3>
              <p className="text-slate-500 dark:text-slate-400 text-xs leading-relaxed">
                TanStack Query optimistic mutations with snapshot caching; automatic rollback and warning toast upon receiving 409 Conflict.
              </p>
            </div>
          </div>

          {/* Verification Matrix Card */}
          <div className="rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900/60 overflow-hidden shadow-lg">
            <div className="px-6 py-4 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between bg-slate-50 dark:bg-slate-900/80">
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-5 h-5 text-emerald-500" />
                <h2 className="font-bold text-xs uppercase tracking-wider text-slate-800 dark:text-slate-200">
                  Phase 5 Verification Matrix
                </h2>
              </div>
              <span className="text-xs font-mono text-emerald-600 dark:text-emerald-400 font-semibold">
                All Specifications Verified
              </span>
            </div>

            <div className="p-6 space-y-3 font-mono text-xs">
              <div className="flex items-center justify-between py-2 border-b border-slate-100 dark:border-slate-800/50">
                <div className="flex items-center gap-2 text-slate-700 dark:text-slate-300">
                  <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                  <span>Flyway Read-Optimized View (V006)</span>
                </div>
                <span className="text-slate-500 dark:text-slate-400">V006__create_board_view.sql</span>
              </div>

              <div className="flex items-center justify-between py-2 border-b border-slate-100 dark:border-slate-800/50">
                <div className="flex items-center gap-2 text-slate-700 dark:text-slate-300">
                  <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                  <span>Kanban Board Projection Endpoint</span>
                </div>
                <span className="text-slate-500 dark:text-slate-400">GET /api/v1/projects/{'{id}'}/board</span>
              </div>

              <div className="flex items-center justify-between py-2 border-b border-slate-100 dark:border-slate-800/50">
                <div className="flex items-center gap-2 text-slate-700 dark:text-slate-300">
                  <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                  <span>FSM Workflow Transitions Endpoint</span>
                </div>
                <span className="text-slate-500 dark:text-slate-400">POST /api/v1/issues/{'{id}'}/transitions</span>
              </div>

              <div className="flex items-center justify-between py-2 border-b border-slate-100 dark:border-slate-800/50">
                <div className="flex items-center gap-2 text-slate-700 dark:text-slate-300">
                  <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                  <span>Optimistic Drag-and-Drop & Rollback</span>
                </div>
                <span className="text-slate-500 dark:text-slate-400">TanStack Query onMutate + onError</span>
              </div>

              <div className="flex items-center justify-between py-2">
                <div className="flex items-center gap-2 text-slate-700 dark:text-slate-300">
                  <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                  <span>Dual-Theme Design & Keyboard Shortcuts</span>
                </div>
                <span className="text-slate-500 dark:text-slate-400">Cmd+T (Theme), C (New Issue), 1/2 (Tabs)</span>
              </div>
            </div>
          </div>
        </main>
      )}
    </div>
  );
};
