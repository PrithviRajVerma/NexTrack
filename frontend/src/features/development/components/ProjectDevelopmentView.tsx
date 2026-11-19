import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { 
  GitBranch, 
  GitCommit, 
  GitPullRequest, 
  ExternalLink, 
  CheckCircle2, 
  Layers, 
  RefreshCw,
  FolderGit2,
  Loader2
} from 'lucide-react';
import { developmentApi } from '../services/developmentApi';
import { ProjectDevelopmentOverview, ConnectedRepository, LinkedCommit, LinkedPullRequest, LinkedBranch } from '../types/development.types';
import { formatDate } from '@/utils/formatDate';

interface ProjectDevelopmentViewProps {
  projectId: string;
  projectKey: string;
  projectName: string;
}

export const ProjectDevelopmentView: React.FC<ProjectDevelopmentViewProps> = ({
  projectId,
  projectKey,
  projectName,
}) => {
  const {
    data: overview,
    isLoading,
    refetch,
    isRefetching,
  } = useQuery<ProjectDevelopmentOverview>({
    queryKey: ['project-development-overview', projectId],
    queryFn: () => developmentApi.fetchProjectDevelopmentOverview(projectId),
    enabled: !!projectId,
  });

  const summary = overview?.summary || {
    totalRepositories: 0,
    totalBranches: 0,
    totalCommits: 0,
    totalPullRequests: 0,
    openPullRequests: 0,
    mergedPullRequests: 0,
  };

  const repos = overview?.connectedRepositories || [];
  const commits = overview?.recentCommits || [];
  const pullRequests = overview?.recentPullRequests || [];
  const branches = overview?.recentBranches || [];

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] text-slate-400">
        <Loader2 className="w-8 h-8 animate-spin mb-2" />
        <span className="text-xs font-medium">Loading project telemetry...</span>
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-fade-in p-6 max-w-7xl mx-auto">
      {/* View Header */}
      <div className="flex items-center justify-between pb-4 border-b border-slate-200 dark:border-slate-800">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-xl font-bold text-slate-900 dark:text-white">
              Code & Repository Telemetry
            </h1>
            <span className="font-mono text-xs px-2 py-0.5 rounded bg-brand-50 text-brand-600 dark:bg-brand-950/60 dark:text-brand-400 font-bold border border-brand-200/50 dark:border-brand-800/50">
              {projectKey}
            </span>
          </div>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Real-time GitHub activity, pull request status, and issue traceability for {projectName}.
          </p>
        </div>

        <button
          onClick={() => refetch()}
          disabled={isRefetching}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-slate-700 dark:text-slate-200 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg hover:bg-slate-50 dark:hover:bg-slate-700/80 transition-colors shadow-sm"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isRefetching ? 'animate-spin' : ''}`} />
          <span>Sync Telemetry</span>
        </button>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
        <div className="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-sm">
          <div className="flex items-center gap-2 text-slate-400 mb-1">
            <FolderGit2 className="w-4 h-4 text-brand-500" />
            <span className="text-[11px] font-semibold uppercase tracking-wider">Repositories</span>
          </div>
          <div className="text-2xl font-bold text-slate-900 dark:text-white">
            {summary.totalRepositories}
          </div>
        </div>

        <div className="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-sm">
          <div className="flex items-center gap-2 text-slate-400 mb-1">
            <GitPullRequest className="w-4 h-4 text-purple-500" />
            <span className="text-[11px] font-semibold uppercase tracking-wider">Open PRs</span>
          </div>
          <div className="text-2xl font-bold text-slate-900 dark:text-white">
            {summary.openPullRequests}
          </div>
        </div>

        <div className="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-sm">
          <div className="flex items-center gap-2 text-slate-400 mb-1">
            <CheckCircle2 className="w-4 h-4 text-emerald-500" />
            <span className="text-[11px] font-semibold uppercase tracking-wider">Merged PRs</span>
          </div>
          <div className="text-2xl font-bold text-slate-900 dark:text-white">
            {summary.mergedPullRequests}
          </div>
        </div>

        <div className="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-sm">
          <div className="flex items-center gap-2 text-slate-400 mb-1">
            <GitCommit className="w-4 h-4 text-blue-500" />
            <span className="text-[11px] font-semibold uppercase tracking-wider">Commits</span>
          </div>
          <div className="text-2xl font-bold text-slate-900 dark:text-white">
            {summary.totalCommits}
          </div>
        </div>

        <div className="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-sm">
          <div className="flex items-center gap-2 text-slate-400 mb-1">
            <GitBranch className="w-4 h-4 text-emerald-500" />
            <span className="text-[11px] font-semibold uppercase tracking-wider">Branches</span>
          </div>
          <div className="text-2xl font-bold text-slate-900 dark:text-white">
            {summary.totalBranches}
          </div>
        </div>

        <div className="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-sm">
          <div className="flex items-center gap-2 text-slate-400 mb-1">
            <Layers className="w-4 h-4 text-amber-500" />
            <span className="text-[11px] font-semibold uppercase tracking-wider">Total PRs</span>
          </div>
          <div className="text-2xl font-bold text-slate-900 dark:text-white">
            {summary.totalPullRequests}
          </div>
        </div>
      </div>

      {/* Connected Repositories */}
      <div className="p-5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-sm space-y-3">
        <h2 className="text-xs font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider flex items-center gap-2">
          <FolderGit2 className="w-4 h-4 text-slate-400" />
          <span>Connected GitHub Repositories</span>
        </h2>

        {repos.length === 0 ? (
          <div className="text-xs text-slate-400 py-3 text-center">
            No repositories linked to this project yet. Connect a repository from Workspace Settings.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            {repos.map((r: ConnectedRepository) => (
              <div
                key={r.id}
                className="p-3.5 bg-slate-50/70 dark:bg-slate-800/50 border border-slate-200 dark:border-slate-700/80 rounded-lg flex items-start justify-between"
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-1.5">
                    <span className="font-semibold text-xs text-slate-900 dark:text-white truncate">
                      {r.fullName}
                    </span>
                    <span className="w-2 h-2 rounded-full bg-emerald-500 inline-block" title="Active" />
                  </div>
                  <div className="text-[11px] text-slate-400 flex items-center gap-2">
                    <span className="font-mono bg-slate-200 dark:bg-slate-700 px-1.5 py-0.2 rounded text-[10px]">
                      {r.defaultBranch}
                    </span>
                    <span>•</span>
                    <span>Synced {r.lastSyncedAt ? formatDate(r.lastSyncedAt) : 'Recently'}</span>
                  </div>
                </div>

                <a
                  href={r.htmlUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1"
                >
                  <ExternalLink className="w-3.5 h-3.5" />
                </a>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Grid: Recent Pull Requests & Recent Commits */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Recent Pull Requests */}
        <div className="p-5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-xs font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider flex items-center gap-2">
              <GitPullRequest className="w-4 h-4 text-purple-500" />
              <span>Recent Pull Requests</span>
            </h2>
            <span className="text-[11px] text-slate-400">{pullRequests.length} active</span>
          </div>

          {pullRequests.length === 0 ? (
            <div className="text-xs text-slate-400 py-6 text-center">
              No recent pull requests recorded for linked repositories.
            </div>
          ) : (
            <div className="space-y-2.5">
              {pullRequests.map((pr: LinkedPullRequest) => (
                <div
                  key={pr.id}
                  className="p-3 bg-slate-50/60 dark:bg-slate-800/40 border border-slate-200 dark:border-slate-700/60 rounded-lg hover:border-slate-300 dark:hover:border-slate-600 transition-all space-y-1"
                >
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex items-center gap-2 truncate">
                      <span className="font-mono text-xs font-bold text-slate-700 dark:text-slate-200">
                        #{pr.prNumber}
                      </span>
                      <span className="text-xs font-semibold text-slate-800 dark:text-slate-100 truncate">
                        {pr.title}
                      </span>
                    </div>
                    {pr.status === 'MERGED' ? (
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-purple-100 text-purple-700 dark:bg-purple-950/60 dark:text-purple-300 flex-shrink-0">
                        Merged
                      </span>
                    ) : (
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-100 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-300 flex-shrink-0">
                        Open
                      </span>
                    )}
                  </div>
                  <div className="flex items-center justify-between text-[11px] text-slate-400 pt-1">
                    <span className="font-mono truncate">
                      {pr.sourceBranch} → {pr.targetBranch}
                    </span>
                    <a
                      href={pr.prUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 inline-flex items-center gap-1 font-sans"
                    >
                      <span>View</span>
                      <ExternalLink className="w-3 h-3" />
                    </a>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Recent Commits */}
        <div className="p-5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-xs font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider flex items-center gap-2">
              <GitCommit className="w-4 h-4 text-blue-500" />
              <span>Recent Commit Activity</span>
            </h2>
            <span className="text-[11px] text-slate-400">{commits.length} commits</span>
          </div>

          {commits.length === 0 ? (
            <div className="text-xs text-slate-400 py-6 text-center">
              No recent commits recorded.
            </div>
          ) : (
            <div className="space-y-2.5">
              {commits.map((c: LinkedCommit) => (
                <div
                  key={c.id}
                  className="p-3 bg-slate-50/60 dark:bg-slate-800/40 border border-slate-200 dark:border-slate-700/60 rounded-lg hover:border-slate-300 dark:hover:border-slate-600 transition-all space-y-1"
                >
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-xs font-medium text-slate-800 dark:text-slate-100 truncate">
                      {c.message}
                    </span>
                    <a
                      href={c.commitUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="font-mono text-xs font-bold text-brand-600 dark:text-brand-400 hover:underline flex-shrink-0 inline-flex items-center gap-1"
                    >
                      <span>{c.commitSha.substring(0, 7)}</span>
                      <ExternalLink className="w-3 h-3 text-slate-400" />
                    </a>
                  </div>
                  <div className="flex items-center justify-between text-[11px] text-slate-400 pt-1">
                    <span className="font-medium text-slate-600 dark:text-slate-300">
                      {c.authorName}
                    </span>
                    <span>{formatDate(c.committedAt)}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Active Branches Section */}
      {branches.length > 0 && (
        <div className="p-5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-sm space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="text-xs font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider flex items-center gap-2">
              <GitBranch className="w-4 h-4 text-emerald-500" />
              <span>Active Repository Branches</span>
            </h2>
            <span className="text-[11px] text-slate-400">{branches.length} branches</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-2.5">
            {branches.map((b: LinkedBranch) => (
              <div
                key={b.id}
                className="p-3 bg-slate-50/60 dark:bg-slate-800/40 border border-slate-200 dark:border-slate-700/60 rounded-lg flex items-center justify-between gap-2"
              >
                <div className="truncate space-y-0.5">
                  <div className="flex items-center gap-2 truncate">
                    <span className="font-mono text-xs font-bold text-slate-800 dark:text-slate-100 truncate">
                      {b.name}
                    </span>
                    {b.lastCommitSha && (
                      <span className="text-[10px] font-mono text-slate-400 bg-slate-200 dark:bg-slate-700 px-1 rounded flex-shrink-0">
                        {b.lastCommitSha.substring(0, 7)}
                      </span>
                    )}
                  </div>
                  <span className="text-[10px] text-slate-400 block truncate">
                    {b.repositoryName}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
