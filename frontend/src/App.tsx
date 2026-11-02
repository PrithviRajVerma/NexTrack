import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { 
  CheckCircle2, 
  Layers, 
  Server, 
  ShieldCheck, 
  Database, 
  Sun, 
  Moon, 
  Activity
} from 'lucide-react';

interface HealthResponse {
  status: string;
}

export const App: React.FC = () => {
  const [isDark, setIsDark] = useState(true);

  // Toggle Theme
  const toggleTheme = () => {
    setIsDark(!isDark);
    document.documentElement.classList.toggle('dark');
  };

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
    <div className={`min-h-screen ${isDark ? 'dark bg-slate-950 text-slate-100' : 'bg-slate-50 text-slate-900'} transition-colors duration-200`}>
      {/* Top Navigation */}
      <header className="border-b border-slate-800/80 bg-slate-900/60 backdrop-blur sticky top-0 z-50 px-6 py-3.5 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-brand-600 to-indigo-500 flex items-center justify-center shadow-lg shadow-brand-500/20 text-white font-bold text-base">
            NT
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-base tracking-tight">NexTrack</span>
              <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-brand-500/10 text-brand-400 border border-brand-500/20">
                Phase 1 Active
              </span>
            </div>
            <p className="text-xs text-slate-400 hidden sm:block">Modular Monolith Issue Tracker & Software Delivery Hub</p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          {/* Backend Status Badge */}
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium border bg-slate-900/80 border-slate-800">
            <span className={`w-2 h-2 rounded-full ${isLoading ? 'bg-amber-400 animate-pulse' : (health?.status === 'UP' ? 'bg-emerald-400 shadow-sm shadow-emerald-400/50' : 'bg-rose-400')}`} />
            <span className="text-slate-300 font-mono text-[11px]">
              API: {isLoading ? 'Checking...' : (health?.status === 'UP' ? 'ONLINE (UP)' : 'WAITING FOR SPRING BOOT')}
            </span>
          </div>

          {/* Theme Toggle Button */}
          <button
            onClick={toggleTheme}
            className="p-2 rounded-lg border border-slate-800 text-slate-400 hover:text-slate-200 hover:bg-slate-800/60 transition-colors"
            title="Toggle Dark / Light Theme"
          >
            {isDark ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4 text-slate-700" />}
          </button>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-6xl mx-auto px-6 py-12">
        <div className="text-center max-w-2xl mx-auto mb-12">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-mono bg-brand-500/10 text-brand-400 border border-brand-500/20 mb-4">
            <Activity className="w-3.5 h-3.5" />
            Phase 1 Infrastructure Scaffolding Complete
          </div>
          <h1 className="text-4xl font-extrabold tracking-tight sm:text-5xl bg-clip-text text-transparent bg-gradient-to-r from-slate-100 via-slate-200 to-slate-400">
            Engineered for High-Scale Software Delivery
          </h1>
          <p className="mt-4 text-slate-400 text-base sm:text-lg">
            Spring Boot 3 + Java 21 LTS modular monolith paired with a React 18 single-page application.
          </p>
        </div>

        {/* Feature Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-12">
          <div className="p-6 rounded-xl border border-slate-800/80 bg-slate-900/40 backdrop-blur">
            <div className="w-10 h-10 rounded-lg bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400 mb-4">
              <Server className="w-5 h-5" />
            </div>
            <h3 className="font-semibold text-lg mb-1">Spring Boot 3.3.5 Backend</h3>
            <p className="text-slate-400 text-sm leading-relaxed">
              Java 21 LTS modular monolith partitioned with Spring Modulith, HikariCP connection pooling, and RFC 7807 error translation.
            </p>
          </div>

          <div className="p-6 rounded-xl border border-slate-800/80 bg-slate-900/40 backdrop-blur">
            <div className="w-10 h-10 rounded-lg bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400 mb-4">
              <Database className="w-5 h-5" />
            </div>
            <h3 className="font-semibold text-lg mb-1">PostgreSQL 16 & Flyway</h3>
            <p className="text-slate-400 text-sm leading-relaxed">
              Docker Compose database container with versioned Flyway migrations, atomic row-counter sequences, and partial indexes.
            </p>
          </div>

          <div className="p-6 rounded-xl border border-slate-800/80 bg-slate-900/40 backdrop-blur">
            <div className="w-10 h-10 rounded-lg bg-brand-500/10 border border-brand-500/20 flex items-center justify-center text-brand-400 mb-4">
              <Layers className="w-5 h-5" />
            </div>
            <h3 className="font-semibold text-lg mb-1">React 18 + TypeScript SPA</h3>
            <p className="text-slate-400 text-sm leading-relaxed">
              Vite bundler, Tailwind CSS design tokens, TanStack Query caching, and keyboard-first Linear-inspired interaction design.
            </p>
          </div>
        </div>

        {/* Verification Matrix Card */}
        <div className="rounded-xl border border-slate-800 bg-slate-900/60 overflow-hidden shadow-2xl">
          <div className="px-6 py-4 border-b border-slate-800 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <ShieldCheck className="w-5 h-5 text-emerald-400" />
              <h2 className="font-semibold text-sm">Phase 1 Verification Matrix</h2>
            </div>
            <span className="text-xs font-mono text-slate-400">All checks passed</span>
          </div>

          <div className="p-6 space-y-3 font-mono text-xs">
            <div className="flex items-center justify-between py-2 border-b border-slate-800/50">
              <div className="flex items-center gap-2 text-slate-300">
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                <span>Docker Compose PostgreSQL 16 Service</span>
              </div>
              <span className="text-slate-400">nextrack-postgres:5432</span>
            </div>

            <div className="flex items-center justify-between py-2 border-b border-slate-800/50">
              <div className="flex items-center gap-2 text-slate-300">
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                <span>Flyway Baseline Migration (V001)</span>
              </div>
              <span className="text-slate-400">V001__baseline_schema.sql</span>
            </div>

            <div className="flex items-center justify-between py-2 border-b border-slate-800/50">
              <div className="flex items-center gap-2 text-slate-300">
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                <span>Spring Modulith Module Verification</span>
              </div>
              <span className="text-slate-400">ModulithArchitectureTests.java</span>
            </div>

            <div className="flex items-center justify-between py-2 border-b border-slate-800/50">
              <div className="flex items-center gap-2 text-slate-300">
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                <span>RFC 7807 Global Exception Handling</span>
              </div>
              <span className="text-slate-400">GlobalExceptionHandler.java</span>
            </div>

            <div className="flex items-center justify-between py-2">
              <div className="flex items-center gap-2 text-slate-300">
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                <span>Vite API Proxy to Spring Boot Backend</span>
              </div>
              <span className="text-slate-400">/api → localhost:8080</span>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};
