import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { 
  GitBranch, 
  GitCommit, 
  GitPullRequest, 
  Plus, 
  ExternalLink, 
  Trash2, 
  CheckCircle2, 
  Clock, 
  Loader2, 
  Sparkles,
  Link as LinkIcon
} from 'lucide-react';
import { developmentApi } from '../services/developmentApi';
import { 
  IssueDevelopmentResponse, 
  LinkedBranch, 
  LinkedCommit, 
  LinkedPullRequest,
  PullRequestStatus 
} from '../types/development.types';
import { ManualLinkModal } from './ManualLinkModal';
import { formatDate } from '@/utils/formatDate';

interface IssueDevelopmentPanelProps {
  issueId: string;
  issueKey: string;
  onErrorToast: (title: string, message: string, variant: 'error' | 'warning' | 'success') => void;
  onLinksChanged?: () => void;
}

export const IssueDevelopmentPanel: React.FC<IssueDevelopmentPanelProps> = ({
  issueId,
  issueKey,
  onErrorToast,
  onLinksChanged,
}) => {
  const [isLinkModalOpen, setIsLinkModalOpen] = useState(false);
  const [unlinkingId, setUnlinkingId] = useState<string | null>(null);

  const {
    data: devData,
    isLoading,
    refetch,
    isRefetching,
  } = useQuery<IssueDevelopmentResponse>({
    queryKey: ['issue-development', issueId],
    queryFn: () => developmentApi.fetchIssueDevelopment(issueId),
    enabled: !!issueId,
  });

  const handleUnlink = async (targetId: string, label: string) => {
    try {
      setUnlinkingId(targetId);
      await developmentApi.unlinkArtifact(issueId, targetId);
      onErrorToast('Link Removed', `Unlinked ${label} from ${issueKey}.`, 'success');
      refetch();
      if (onLinksChanged) onLinksChanged();
    } catch (err: any) {
      onErrorToast('Unlink Failed', err.message || 'Could not unlink artifact.', 'error');
    } finally {
      setUnlinkingId(null);
    }
  };

  const renderPRStatusBadge = (status: PullRequestStatus, isDraft: boolean) => {
    if (isDraft) {
      return (
        <span className="px-2 py-0.5 rounded-full text-[10px] font-medium bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-400">
          Draft
        </span>
      );
    }
    switch (status) {
      case 'MERGED':
        return (
          <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-purple-100 text-purple-700 dark:bg-purple-950/60 dark:text-purple-300 flex items-center gap-1">
            <CheckCircle2 className="w-3 h-3" />
            <span>Merged</span>
          </span>
        );
      case 'CLOSED':
        return (
          <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-rose-100 text-rose-700 dark:bg-rose-950/60 dark:text-rose-300">
            Closed
          </span>
        );
      case 'OPEN':
      default:
        return (
          <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-100 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-300 flex items-center gap-1">
            <Clock className="w-3 h-3" />
            <span>Open</span>
          </span>
        );
    }
  };

  const renderLinkTypeBadge = (linkType: string, linkedByName?: string | null) => {
    if (linkType === 'AUTO_EXTRACTED') {
      return (
        <span 
          className="inline-flex items-center gap-1 text-[10px] font-medium px-2 py-0.5 rounded bg-blue-50 dark:bg-blue-950/50 text-blue-600 dark:text-blue-300 border border-blue-200/50 dark:border-blue-900/50"
          title="Extracted automatically from Git telemetry via issue key regex"
        >
          <Sparkles className="w-2.5 h-2.5" />
          <span>Auto-Linked</span>
        </span>
      );
    }
    return (
      <span 
        className="inline-flex items-center gap-1 text-[10px] font-medium px-2 py-0.5 rounded bg-amber-50 dark:bg-amber-950/50 text-amber-700 dark:text-amber-300 border border-amber-200/50 dark:border-amber-900/50"
        title={`Manually linked by ${linkedByName || 'Member'}`}
      >
        <LinkIcon className="w-2.5 h-2.5" />
        <span>Manual {linkedByName ? `by ${linkedByName}` : ''}</span>
      </span>
    );
  };

  const branches = devData?.branches || [];
  const commits = devData?.commits || [];
  const pullRequests = devData?.pullRequests || [];
  const totalCount = branches.length + commits.length + pullRequests.length;

  return (
    <div className="space-y-5 animate-fade-in">
      {/* Top Header & Link Action */}
      <div className="flex items-center justify-between pb-2 border-b border-slate-100 dark:border-slate-800">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="text-sm font-bold text-slate-800 dark:text-slate-100">
              Code & Development Traceability
            </h3>
            {isRefetching && <Loader2 className="w-3.5 h-3.5 text-slate-400 animate-spin" />}
          </div>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            Git branches, commits, and pull requests connected to <span className="font-mono font-semibold">{issueKey}</span>.
          </p>
        </div>

        <button
          onClick={() => setIsLinkModalOpen(true)}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-brand-600 dark:text-brand-400 bg-brand-50 hover:bg-brand-100 dark:bg-brand-950/50 dark:hover:bg-brand-900/50 border border-brand-200 dark:border-brand-800/80 rounded-lg transition-colors shadow-sm"
        >
          <Plus className="w-3.5 h-3.5" />
          <span>Link Code</span>
        </button>
      </div>

      {isLoading ? (
        <div className="flex flex-col items-center justify-center py-12 text-slate-400">
          <Loader2 className="w-6 h-6 animate-spin mb-2" />
          <span className="text-xs font-medium">Loading development telemetry...</span>
        </div>
      ) : totalCount === 0 ? (
        <div className="flex flex-col items-center justify-center py-10 px-4 rounded-xl border border-dashed border-slate-200 dark:border-slate-800 text-center bg-slate-50/50 dark:bg-slate-900/30">
          <GitBranch className="w-8 h-8 text-slate-400 mb-2" />
          <h4 className="text-xs font-bold text-slate-700 dark:text-slate-200 mb-1">
            No code telemetry linked to this issue yet
          </h4>
          <p className="text-[11px] text-slate-500 dark:text-slate-400 max-w-sm mb-4">
            Push commits or create branches referencing <span className="font-mono font-semibold text-slate-700 dark:text-slate-300">[{issueKey}]</span> to link automatically, or link an existing artifact manually.
          </p>
          <button
            onClick={() => setIsLinkModalOpen(true)}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-white bg-brand-600 hover:bg-brand-700 rounded-lg shadow-sm transition-colors"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>Link Commit, PR, or Branch</span>
          </button>
        </div>
      ) : (
        <div className="space-y-6">
          {/* Pull Requests Section */}
          {pullRequests.length > 0 && (
            <div className="space-y-2">
              <div className="flex items-center gap-2 text-xs font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider">
                <GitPullRequest className="w-3.5 h-3.5 text-purple-500" />
                <span>Pull Requests ({pullRequests.length})</span>
              </div>
              <div className="space-y-2">
                {pullRequests.map((pr: LinkedPullRequest) => (
                  <div
                    key={pr.id}
                    className="p-3 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-sm hover:border-slate-300 dark:hover:border-slate-700 transition-all flex items-start justify-between gap-3"
                  >
                    <div className="space-y-1.5 flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        {renderPRStatusBadge(pr.status, pr.isDraft)}
                        <a
                          href={pr.prUrl}
                          target="_blank"
                          rel="noreferrer"
                          className="font-mono text-xs font-bold text-slate-800 dark:text-slate-100 hover:text-brand-600 dark:hover:text-brand-400 inline-flex items-center gap-1"
                        >
                          <span>#{pr.prNumber}</span>
                          <ExternalLink className="w-3 h-3 text-slate-400" />
                        </a>
                        <span className="text-xs font-semibold text-slate-800 dark:text-slate-200 truncate">
                          {pr.title}
                        </span>
                      </div>
                      <div className="flex items-center gap-2 text-[11px] text-slate-500 dark:text-slate-400 flex-wrap">
                        <span className="font-mono bg-slate-100 dark:bg-slate-800 px-1.5 py-0.5 rounded">
                          {pr.sourceBranch} → {pr.targetBranch}
                        </span>
                        <span>•</span>
                        <span>{pr.repositoryName}</span>
                        <span>•</span>
                        <span>{formatDate(pr.linkedAt)}</span>
                        {renderLinkTypeBadge(pr.linkType, pr.linkedByName)}
                      </div>
                    </div>

                    <button
                      onClick={() => handleUnlink(pr.pullRequestId, `PR #${pr.prNumber}`)}
                      disabled={unlinkingId === pr.pullRequestId}
                      title="Unlink pull request"
                      className="p-1.5 text-slate-400 hover:text-rose-500 hover:bg-rose-50 dark:hover:bg-rose-950/30 rounded-lg transition-colors flex-shrink-0"
                    >
                      {unlinkingId === pr.pullRequestId ? (
                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      ) : (
                        <Trash2 className="w-3.5 h-3.5" />
                      )}
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Commits Section */}
          {commits.length > 0 && (
            <div className="space-y-2">
              <div className="flex items-center gap-2 text-xs font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider">
                <GitCommit className="w-3.5 h-3.5 text-blue-500" />
                <span>Commits ({commits.length})</span>
              </div>
              <div className="space-y-2">
                {commits.map((c: LinkedCommit) => (
                  <div
                    key={c.id}
                    className="p-3 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-sm hover:border-slate-300 dark:hover:border-slate-700 transition-all flex items-start justify-between gap-3"
                  >
                    <div className="space-y-1.5 flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <a
                          href={c.commitUrl}
                          target="_blank"
                          rel="noreferrer"
                          className="font-mono text-xs font-bold text-brand-600 dark:text-brand-400 hover:underline inline-flex items-center gap-1"
                        >
                          <span>{c.commitSha.substring(0, 8)}</span>
                          <ExternalLink className="w-3 h-3 text-slate-400" />
                        </a>
                        <span className="text-xs text-slate-700 dark:text-slate-200 font-medium truncate">
                          {c.message}
                        </span>
                      </div>
                      <div className="flex items-center gap-2 text-[11px] text-slate-500 dark:text-slate-400 flex-wrap">
                        <span className="font-medium text-slate-700 dark:text-slate-300">
                          {c.authorName}
                        </span>
                        <span>•</span>
                        <span>{c.repositoryName}</span>
                        <span>•</span>
                        <span>{formatDate(c.committedAt)}</span>
                        {renderLinkTypeBadge(c.linkType, c.linkedByName)}
                      </div>
                    </div>

                    <button
                      onClick={() => handleUnlink(c.commitId, `Commit ${c.commitSha.substring(0, 7)}`)}
                      disabled={unlinkingId === c.commitId}
                      title="Unlink commit"
                      className="p-1.5 text-slate-400 hover:text-rose-500 hover:bg-rose-50 dark:hover:bg-rose-950/30 rounded-lg transition-colors flex-shrink-0"
                    >
                      {unlinkingId === c.commitId ? (
                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      ) : (
                        <Trash2 className="w-3.5 h-3.5" />
                      )}
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Branches Section */}
          {branches.length > 0 && (
            <div className="space-y-2">
              <div className="flex items-center gap-2 text-xs font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider">
                <GitBranch className="w-3.5 h-3.5 text-emerald-500" />
                <span>Branches ({branches.length})</span>
              </div>
              <div className="space-y-2">
                {branches.map((b: LinkedBranch) => (
                  <div
                    key={b.id}
                    className="p-3 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-sm hover:border-slate-300 dark:hover:border-slate-700 transition-all flex items-center justify-between gap-3"
                  >
                    <div className="space-y-1 flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-bold text-slate-800 dark:text-slate-200 truncate">
                          {b.name}
                        </span>
                        {b.lastCommitSha && (
                          <span className="text-[10px] font-mono text-slate-400 bg-slate-100 dark:bg-slate-800 px-1 rounded">
                            {b.lastCommitSha.substring(0, 7)}
                          </span>
                        )}
                      </div>
                      <div className="flex items-center gap-2 text-[11px] text-slate-500 dark:text-slate-400 flex-wrap">
                        <span>{b.repositoryName}</span>
                        <span>•</span>
                        <span>{formatDate(b.linkedAt)}</span>
                        {renderLinkTypeBadge(b.linkType, b.linkedByName)}
                      </div>
                    </div>

                    <button
                      onClick={() => handleUnlink(b.branchId, `Branch ${b.name}`)}
                      disabled={unlinkingId === b.branchId}
                      title="Unlink branch"
                      className="p-1.5 text-slate-400 hover:text-rose-500 hover:bg-rose-50 dark:hover:bg-rose-950/30 rounded-lg transition-colors flex-shrink-0"
                    >
                      {unlinkingId === b.branchId ? (
                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      ) : (
                        <Trash2 className="w-3.5 h-3.5" />
                      )}
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Manual Link Modal */}
      <ManualLinkModal
        isOpen={isLinkModalOpen}
        issueId={issueId}
        issueKey={issueKey}
        onClose={() => setIsLinkModalOpen(false)}
        onSuccess={() => {
          refetch();
          if (onLinksChanged) onLinksChanged();
        }}
        onError={onErrorToast}
      />
    </div>
  );
};
