import React, { useState, useEffect } from 'react';
import { 
  X, 
  Search, 
  GitCommit, 
  GitPullRequest, 
  GitBranch, 
  Loader2, 
  AlertCircle, 
  Check, 
  ExternalLink 
} from 'lucide-react';
import { developmentApi } from '../services/developmentApi';
import { DevelopmentCandidate, ManualLinkPayload } from '../types/development.types';

interface ManualLinkModalProps {
  isOpen: boolean;
  issueId: string;
  issueKey: string;
  onClose: () => void;
  onSuccess: () => void;
  onError: (title: string, message: string, variant: 'error' | 'warning' | 'success') => void;
}

type TabType = 'COMMIT' | 'PULL_REQUEST' | 'BRANCH';

export const ManualLinkModal: React.FC<ManualLinkModalProps> = ({
  isOpen,
  issueId,
  issueKey,
  onClose,
  onSuccess,
  onError,
}) => {
  const [activeTab, setActiveTab] = useState<TabType>('COMMIT');
  const [searchQuery, setSearchQuery] = useState('');
  const [candidates, setCandidates] = useState<DevelopmentCandidate[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [selectedCandidate, setSelectedCandidate] = useState<DevelopmentCandidate | null>(null);
  const [customIdentifier, setCustomIdentifier] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Debounced search candidates
  useEffect(() => {
    if (!isOpen) return;

    let isMounted = true;
    setIsLoading(true);

    const timer = setTimeout(async () => {
      try {
        const results = await developmentApi.searchCandidates(issueId, activeTab, searchQuery);
        if (isMounted) {
          setCandidates(results);
          setIsLoading(false);
        }
      } catch (err: any) {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }, 250);

    return () => {
      isMounted = false;
      clearTimeout(timer);
    };
  }, [isOpen, issueId, activeTab, searchQuery]);

  // Reset state on open/tab change
  useEffect(() => {
    setSelectedCandidate(null);
    setCustomIdentifier('');
  }, [activeTab, isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedCandidate && !customIdentifier.trim()) {
      onError('Selection Required', 'Please select a candidate or enter an identifier.', 'warning');
      return;
    }

    setIsSubmitting(true);
    try {
      const payload: ManualLinkPayload = {
        artifactType: activeTab,
        artifactId: selectedCandidate?.id || null,
        identifier: selectedCandidate ? null : customIdentifier.trim(),
        repositoryId: selectedCandidate?.repositoryId || null,
      };

      await developmentApi.linkArtifact(issueId, payload);
      onError('Artifact Linked', `Successfully linked ${activeTab.toLowerCase()} to ${issueKey}.`, 'success');
      onSuccess();
      onClose();
    } catch (err: any) {
      if (err.response?.status === 409 || err.status === 409) {
        onError('Already Linked (409)', `This ${activeTab.toLowerCase()} is already associated with this issue.`, 'warning');
      } else if (err.response?.status === 404 || err.status === 404) {
        onError('Artifact Not Found (404)', `Could not resolve the specified ${activeTab.toLowerCase()} in connected repositories.`, 'error');
      } else {
        onError('Link Failed', err.response?.data?.detail || err.message || 'Unable to create development link.', 'error');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-fade-in">
      <div 
        className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl w-full max-w-xl shadow-2xl flex flex-col overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 dark:border-slate-800">
          <div>
            <h3 className="text-base font-bold text-slate-900 dark:text-white">
              Link Code Artifact to {issueKey}
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Manual fallback linking for commits, PRs, or branches without key annotations.
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab Selector */}
        <div className="flex border-b border-slate-200 dark:border-slate-800 px-6 pt-3 gap-6 bg-slate-50/50 dark:bg-slate-800/20">
          <button
            type="button"
            onClick={() => setActiveTab('COMMIT')}
            className={`flex items-center gap-2 pb-3 text-xs font-semibold border-b-2 transition-all ${
              activeTab === 'COMMIT'
                ? 'border-brand-500 text-brand-600 dark:text-brand-400'
                : 'border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-300'
            }`}
          >
            <GitCommit className="w-4 h-4" />
            <span>Commit</span>
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('PULL_REQUEST')}
            className={`flex items-center gap-2 pb-3 text-xs font-semibold border-b-2 transition-all ${
              activeTab === 'PULL_REQUEST'
                ? 'border-brand-500 text-brand-600 dark:text-brand-400'
                : 'border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-300'
            }`}
          >
            <GitPullRequest className="w-4 h-4" />
            <span>Pull Request</span>
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('BRANCH')}
            className={`flex items-center gap-2 pb-3 text-xs font-semibold border-b-2 transition-all ${
              activeTab === 'BRANCH'
                ? 'border-brand-500 text-brand-600 dark:text-brand-400'
                : 'border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-300'
            }`}
          >
            <GitBranch className="w-4 h-4" />
            <span>Branch</span>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4 flex-1 flex flex-col overflow-hidden">
          {/* Search Input */}
          <div className="relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder={
                activeTab === 'COMMIT'
                  ? 'Filter by commit SHA or commit message...'
                  : activeTab === 'PULL_REQUEST'
                  ? 'Filter by PR number or title...'
                  : 'Filter by branch name...'
              }
              className="w-full pl-9 pr-4 py-2 text-xs bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>

          {/* Candidate List */}
          <div className="flex-1 min-h-[180px] max-h-[240px] overflow-y-auto space-y-1.5 border border-slate-100 dark:border-slate-800 rounded-xl p-2 bg-slate-50/40 dark:bg-slate-950/40">
            {isLoading ? (
              <div className="flex flex-col items-center justify-center h-full py-8 text-slate-400">
                <Loader2 className="w-5 h-5 animate-spin mb-2" />
                <span className="text-xs">Searching connected repositories...</span>
              </div>
            ) : candidates.length === 0 ? (
              <div className="flex flex-col items-center justify-center h-full py-8 text-slate-400 text-center px-4">
                <AlertCircle className="w-6 h-6 mb-1 text-slate-300 dark:text-slate-600" />
                <span className="text-xs font-medium text-slate-500 dark:text-slate-400">
                  No matching candidates found
                </span>
                <span className="text-[11px] text-slate-400 mt-0.5">
                  You can specify an exact SHA, branch name, or PR number below.
                </span>
              </div>
            ) : (
              candidates.map((cand) => {
                const isSelected = selectedCandidate?.id === cand.id;
                return (
                  <div
                    key={cand.id}
                    onClick={() => {
                      setSelectedCandidate(cand);
                      setCustomIdentifier('');
                    }}
                    className={`flex items-start justify-between p-2.5 rounded-lg cursor-pointer transition-all border ${
                      isSelected
                        ? 'bg-brand-50/80 dark:bg-brand-950/40 border-brand-500/50 text-brand-900 dark:text-brand-100'
                        : 'bg-white dark:bg-slate-800/80 border-slate-200/80 dark:border-slate-700/60 hover:border-slate-300 dark:hover:border-slate-600'
                    }`}
                  >
                    <div className="flex items-start gap-2.5 overflow-hidden">
                      <div className="mt-0.5">
                        {activeTab === 'COMMIT' && <GitCommit className="w-4 h-4 text-slate-500" />}
                        {activeTab === 'PULL_REQUEST' && <GitPullRequest className="w-4 h-4 text-purple-500" />}
                        {activeTab === 'BRANCH' && <GitBranch className="w-4 h-4 text-emerald-500" />}
                      </div>
                      <div className="overflow-hidden">
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-xs font-bold text-slate-700 dark:text-slate-200">
                            {cand.identifier}
                          </span>
                          <span className="text-[10px] text-slate-400 font-medium truncate">
                            {cand.repositoryName}
                          </span>
                        </div>
                        <p className="text-xs text-slate-600 dark:text-slate-300 line-clamp-1 mt-0.5">
                          {cand.title}
                        </p>
                        {cand.subtitle && (
                          <span className="text-[10px] text-slate-400 block mt-0.5">
                            {cand.subtitle}
                          </span>
                        )}
                      </div>
                    </div>

                    <div className="flex items-center gap-2 flex-shrink-0 ml-3">
                      {cand.url && (
                        <a
                          href={cand.url}
                          target="_blank"
                          rel="noreferrer"
                          onClick={(e) => e.stopPropagation()}
                          className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1"
                        >
                          <ExternalLink className="w-3.5 h-3.5" />
                        </a>
                      )}
                      <div
                        className={`w-4 h-4 rounded-full flex items-center justify-center border ${
                          isSelected
                            ? 'bg-brand-500 border-brand-500 text-white'
                            : 'border-slate-300 dark:border-slate-600'
                        }`}
                      >
                        {isSelected && <Check className="w-2.5 h-2.5 stroke-[3]" />}
                      </div>
                    </div>
                  </div>
                );
              })
            )}
          </div>

          {/* Or Manual Identifier Input */}
          <div className="pt-2">
            <label className="block text-[11px] font-semibold text-slate-600 dark:text-slate-400 mb-1">
              Or Enter Exact Identifier:
            </label>
            <input
              type="text"
              value={customIdentifier}
              onChange={(e) => {
                setCustomIdentifier(e.target.value);
                if (e.target.value.trim()) {
                  setSelectedCandidate(null);
                }
              }}
              placeholder={
                activeTab === 'COMMIT'
                  ? 'e.g. Full SHA or 7+ character prefix: 4a9f3b1...'
                  : activeTab === 'PULL_REQUEST'
                  ? 'e.g. Pull request number: #14 or 14'
                  : 'e.g. Exact branch name: feature/user-profile'
              }
              className="w-full px-3 py-2 text-xs font-mono bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>

          {/* Actions */}
          <div className="flex items-center justify-end gap-2.5 pt-4 border-t border-slate-100 dark:border-slate-800">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-3.5 py-1.5 text-xs font-medium text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting || (!selectedCandidate && !customIdentifier.trim())}
              className="px-4 py-1.5 text-xs font-semibold text-white bg-brand-600 hover:bg-brand-700 disabled:opacity-50 disabled:cursor-not-allowed rounded-lg shadow-sm flex items-center gap-1.5 transition-all"
            >
              {isSubmitting && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
              <span>Link Artifact</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
