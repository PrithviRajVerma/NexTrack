import React from 'react';
import { Activity, Server, Database, Layers, ShieldCheck, CheckCircle2 } from 'lucide-react';

export const ArchitectureDashboard: React.FC = () => {
  return (
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
  );
};
