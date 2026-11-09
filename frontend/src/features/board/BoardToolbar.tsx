import React from 'react';
import { IssuePriority, IssueType } from '../../types/board';
import { Search, Plus, Filter, X, GitPullRequest, ShieldAlert } from 'lucide-react';

interface BoardToolbarProps {
  searchQuery: string;
  onSearchChange: (query: string) => void;
  selectedPriority: IssuePriority | 'ALL';
  onPriorityChange: (priority: IssuePriority | 'ALL') => void;
  selectedType: IssueType | 'ALL';
  onTypeChange: (type: IssueType | 'ALL') => void;
  onlyWithPRs: boolean;
  onToggleOnlyWithPRs: () => void;
  simulateConflict: boolean;
  onToggleSimulateConflict: () => void;
  onOpenCreateModal: () => void;
  onResetFilters: () => void;
  totalVisible: number;
  totalAll: number;
}

export const BoardToolbar: React.FC<BoardToolbarProps> = ({
  searchQuery,
  onSearchChange,
  selectedPriority,
  onPriorityChange,
  selectedType,
  onTypeChange,
  onlyWithPRs,
  onToggleOnlyWithPRs,
  simulateConflict,
  onToggleSimulateConflict,
  onOpenCreateModal,
  onResetFilters,
  totalVisible,
  totalAll,
}) => {
  const hasActiveFilters = searchQuery !== '' || selectedPriority !== 'ALL' || selectedType !== 'ALL' || onlyWithPRs;

  return (
    <div className="border-b border-slate-200 dark:border-slate-800 bg-white/70 dark:bg-slate-900/60 backdrop-blur px-6 py-3 flex flex-wrap items-center justify-between gap-3 sticky top-[57px] z-20">
      {/* Left side: Search & Filter Chips */}
      <div className="flex flex-wrap items-center gap-2.5 flex-1 min-w-[280px]">
        {/* Search Input */}
        <div className="relative w-64 max-w-full">
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
            placeholder="Search key or title... (e.g. NT-101)"
            className="w-full pl-9 pr-3 py-1.5 rounded-lg text-xs bg-slate-100 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 focus:outline-none focus:ring-2 focus:ring-brand-500 text-slate-900 dark:text-slate-100 placeholder-slate-400"
          />
          {searchQuery && (
            <button
              onClick={() => onSearchChange('')}
              className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </div>

        {/* Priority Filter */}
        <div className="flex items-center gap-1 bg-slate-100 dark:bg-slate-800/80 p-1 rounded-lg border border-slate-200 dark:border-slate-700 text-xs">
          <Filter className="w-3.5 h-3.5 text-slate-400 ml-1.5" />
          <select
            value={selectedPriority}
            onChange={(e) => onPriorityChange(e.target.value as IssuePriority | 'ALL')}
            className="bg-transparent border-none text-xs text-slate-700 dark:text-slate-300 focus:outline-none pr-1 cursor-pointer font-medium"
          >
            <option value="ALL">All Priorities</option>
            <option value="URGENT">Urgent</option>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
          </select>
        </div>

        {/* Type Filter */}
        <div className="flex items-center gap-1 bg-slate-100 dark:bg-slate-800/80 p-1 rounded-lg border border-slate-200 dark:border-slate-700 text-xs">
          <select
            value={selectedType}
            onChange={(e) => onTypeChange(e.target.value as IssueType | 'ALL')}
            className="bg-transparent border-none text-xs text-slate-700 dark:text-slate-300 focus:outline-none px-2 cursor-pointer font-medium"
          >
            <option value="ALL">All Types</option>
            <option value="TASK">Task</option>
            <option value="BUG">Bug</option>
            <option value="STORY">Story</option>
            <option value="EPIC">Epic</option>
          </select>
        </div>

        {/* Linked PRs Toggle */}
        <button
          onClick={onToggleOnlyWithPRs}
          className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium border transition-colors ${
            onlyWithPRs
              ? 'bg-purple-500/10 border-purple-500/40 text-purple-600 dark:text-purple-400'
              : 'bg-slate-100 dark:bg-slate-800/80 border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-400 hover:border-slate-300'
          }`}
        >
          <GitPullRequest className="w-3.5 h-3.5" />
          <span>Linked PRs</span>
        </button>

        {hasActiveFilters && (
          <button
            onClick={onResetFilters}
            className="text-xs text-brand-600 dark:text-brand-400 hover:underline px-1 font-medium"
          >
            Clear filters
          </button>
        )}

        {/* Visible count indicator */}
        <span className="text-xs text-slate-400 font-mono ml-auto hidden md:inline">
          Showing {totalVisible} of {totalAll} issues
        </span>
      </div>

      {/* Right side: 409 Conflict Simulation Mode Toggle & Create Issue Button */}
      <div className="flex items-center gap-2.5">
        {/* Simulate 409 Conflict Button */}
        <button
          onClick={onToggleSimulateConflict}
          title="When enabled, next drag-drop sends stale version to trigger 409 Conflict rollback"
          className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium border transition-all ${
            simulateConflict
              ? 'bg-rose-500/15 border-rose-500/50 text-rose-600 dark:text-rose-400 animate-pulse'
              : 'bg-slate-100 dark:bg-slate-800/80 border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-400 hover:border-slate-300'
          }`}
        >
          <ShieldAlert className="w-3.5 h-3.5" />
          <span>{simulateConflict ? 'Conflict Mode: ON' : 'Test 409 Conflict'}</span>
        </button>

        {/* Create Issue Button */}
        <button
          onClick={onOpenCreateModal}
          className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-semibold bg-brand-600 hover:bg-brand-500 text-white shadow-md shadow-brand-500/20 transition-all active:scale-95"
        >
          <Plus className="w-3.5 h-3.5" />
          <span>New Issue</span>
          <span className="text-[10px] bg-brand-700/60 px-1 rounded ml-1 font-mono hidden sm:inline">C</span>
        </button>
      </div>
    </div>
  );
};
