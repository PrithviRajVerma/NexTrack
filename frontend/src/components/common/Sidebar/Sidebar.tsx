import React from 'react';
import { Kanban, ShieldCheck, GitPullRequest, Settings, Users, FolderGit2 } from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';

export const Sidebar: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const links = [
    { label: 'Kanban Board', path: '/', icon: Kanban },
    { label: 'Architecture', path: '/dashboard', icon: ShieldCheck },
    { label: 'Repositories', path: '/repositories', icon: FolderGit2, disabled: true },
    { label: 'Pull Requests', path: '/pull-requests', icon: GitPullRequest, disabled: true },
    { label: 'Members', path: '/members', icon: Users, disabled: true },
    { label: 'Settings', path: '/settings', icon: Settings, disabled: true },
  ];

  return (
    <aside className="w-56 border-r border-slate-200 dark:border-slate-800 bg-white/50 dark:bg-slate-900/40 p-4 hidden md:flex flex-col justify-between">
      <div className="space-y-1">
        <div className="px-3 py-2 text-[10px] font-bold uppercase tracking-wider text-slate-400">
          Navigation
        </div>
        {links.map((link) => {
          const Icon = link.icon;
          const isActive = location.pathname === link.path;
          return (
            <button
              key={link.label}
              disabled={link.disabled}
              onClick={() => !link.disabled && navigate(link.path)}
              className={`w-full flex items-center justify-between px-3 py-2 rounded-lg text-xs font-medium transition-colors ${
                isActive
                  ? 'bg-brand-500/10 text-brand-600 dark:text-brand-400 font-bold'
                  : link.disabled
                  ? 'opacity-40 cursor-not-allowed text-slate-400'
                  : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800/80 hover:text-slate-900 dark:hover:text-slate-100'
              }`}
            >
              <div className="flex items-center gap-2.5">
                <Icon className="w-4 h-4" />
                <span>{link.label}</span>
              </div>
              {link.disabled && (
                <span className="text-[9px] font-mono uppercase bg-slate-100 dark:bg-slate-800 px-1.5 py-0.2 rounded text-slate-400">
                  Soon
                </span>
              )}
            </button>
          );
        })}
      </div>

      <div className="p-3 rounded-xl bg-slate-100/60 dark:bg-slate-800/40 border border-slate-200/80 dark:border-slate-800 text-[11px] text-slate-500 dark:text-slate-400">
        <span className="font-semibold text-slate-800 dark:text-slate-200 block mb-0.5">NexTrack Hub</span>
        <span>Version 0.5.0 (Phase 5)</span>
      </div>
    </aside>
  );
};
