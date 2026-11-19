import React, { useState, useRef } from 'react';
import { BoardCard, IssueStatus, IssueComment, AuditEvent, IssueAttachment } from '../types/board.types';
import { 
  MessageSquare, 
  History, 
  AlertTriangle, 
  Send, 
  ShieldCheck, 
  ArrowRight,
  Paperclip,
  Download,
  Trash2,
  FileText,
  Image as ImageIcon,
  FileArchive,
  UploadCloud,
  File as FileGenericIcon,
  GitBranch
} from 'lucide-react';
import { boardApi } from '../services/boardApi';
import { IssueDevelopmentPanel } from '@/features/development';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Modal } from '@/components/ui/Modal';
import { Button } from '@/components/ui/Button';
import { formatDate, formatTime } from '@/utils/formatDate';

interface IssueDetailModalProps {
  card: BoardCard | null;
  isOpen: boolean;
  onClose: () => void;
  onStatusChanged: (cardId: string, newStatus: IssueStatus, newVersion: number) => void;
  onErrorToast: (title: string, message: string, type: 'warning' | 'error' | 'success') => void;
}

export const IssueDetailModal: React.FC<IssueDetailModalProps> = ({
  card,
  isOpen,
  onClose,
  onStatusChanged,
  onErrorToast,
}) => {
  const [activeTab, setActiveTab] = useState<'details' | 'comments' | 'history' | 'attachments' | 'development'>('details');
  const [newCommentText, setNewCommentText] = useState('');
  const [isTransitioning, setIsTransitioning] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [isDragOver, setIsDragOver] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const queryClient = useQueryClient();

  // Comments Query
  const { data: comments = [], refetch: refetchComments, isLoading: commentsLoading } = useQuery<IssueComment[]>({
    queryKey: ['issue-comments', card?.id],
    queryFn: () => (card ? boardApi.fetchComments(card.id) : Promise.resolve([])),
    enabled: isOpen && !!card && activeTab === 'comments',
  });

  // History Query
  const { data: auditHistory = [], isLoading: historyLoading } = useQuery<AuditEvent[]>({
    queryKey: ['issue-history', card?.id],
    queryFn: () => (card ? boardApi.fetchIssueHistory(card.id) : Promise.resolve([])),
    enabled: isOpen && !!card && activeTab === 'history',
  });

  // Attachments Query
  const { data: attachments = [], refetch: refetchAttachments, isLoading: attachmentsLoading } = useQuery<IssueAttachment[]>({
    queryKey: ['issue-attachments', card?.id],
    queryFn: () => (card ? boardApi.fetchAttachments(card.id) : Promise.resolve([])),
    enabled: isOpen && !!card && (activeTab === 'attachments' || activeTab === 'details'),
  });

  if (!isOpen || !card) return null;

  // Handle Status Transition
  const handleTransition = async (targetStatus: IssueStatus, sendStaleVersion: boolean = false) => {
    try {
      setIsTransitioning(true);
      const effectiveVersion = sendStaleVersion ? Math.max(0, card.version - 1) : card.version;
      const updated = await boardApi.transitionIssue(card.id, {
        targetStatus,
        version: effectiveVersion,
      });

      onStatusChanged(card.id, updated.status, updated.version);
      queryClient.invalidateQueries({ queryKey: ['issue-history', card.id] });
    } catch (err: any) {
      if (err.status === 409) {
        onErrorToast(
          'Optimistic Lock Conflict (409)',
          'Issue version was modified concurrently. Rolling back state to preserve consistency.',
          'warning'
        );
      } else {
        onErrorToast(
          'Transition Denied (400)',
          err.message || 'Workflow state machine disallowed this transition.',
          'error'
        );
      }
    } finally {
      setIsTransitioning(false);
    }
  };

  // Add Comment
  const handleAddComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCommentText.trim()) return;

    try {
      await boardApi.addComment(card.id, newCommentText.trim());
      setNewCommentText('');
      refetchComments();
      queryClient.invalidateQueries({ queryKey: ['issue-history', card.id] });
    } catch (err: any) {
      onErrorToast('Comment Failed', err.message || 'Could not post comment', 'error');
    }
  };

  // Handle File Upload
  const handleFileUpload = async (file: File) => {
    if (file.size > 10 * 1024 * 1024) {
      onErrorToast('File Too Large', 'Attachments must be 10 MB or smaller.', 'error');
      return;
    }

    try {
      setIsUploading(true);
      await boardApi.uploadAttachment(card.id, file);
      refetchAttachments();
      queryClient.invalidateQueries({ queryKey: ['board-view'] });
      queryClient.invalidateQueries({ queryKey: ['issue-history', card.id] });
    } catch (err: any) {
      onErrorToast(
        'Upload Rejected',
        err.message || 'File upload failed. Verify file is not spoofed and matches allowed types.',
        'error'
      );
    } finally {
      setIsUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const handleDownload = async (attachment: IssueAttachment) => {
    try {
      await boardApi.downloadAttachment(card.id, attachment.id, attachment.originalFilename);
    } catch (err: any) {
      onErrorToast('Download Failed', err.message || 'Failed to download attachment', 'error');
    }
  };

  const handleDeleteAttachment = async (attachmentId: string) => {
    try {
      await boardApi.deleteAttachment(card.id, attachmentId);
      refetchAttachments();
      queryClient.invalidateQueries({ queryKey: ['board-view'] });
      queryClient.invalidateQueries({ queryKey: ['issue-history', card.id] });
    } catch (err: any) {
      onErrorToast('Delete Failed', err.message || 'Failed to delete attachment', 'error');
    }
  };

  const formatBytes = (bytes: number): string => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  };

  const getFileIcon = (contentType: string) => {
    if (contentType.startsWith('image/')) return <ImageIcon className="w-4 h-4 text-purple-500" />;
    if (contentType === 'application/pdf') return <FileText className="w-4 h-4 text-red-500" />;
    if (contentType.includes('zip')) return <FileArchive className="w-4 h-4 text-amber-500" />;
    return <FileGenericIcon className="w-4 h-4 text-blue-500" />;
  };

  const statusOptions: IssueStatus[] = ['BACKLOG', 'TO_DO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE', 'ARCHIVED'];

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={
        <div className="flex items-center gap-2.5">
          <span className="font-mono text-xs font-bold px-2 py-0.5 rounded-full bg-brand-500/10 text-brand-600 dark:text-brand-400 border border-brand-500/20">
            {card.formattedKey}
          </span>
          <span className="text-xs font-mono text-slate-400 border border-slate-200 dark:border-slate-700 px-1.5 py-0.5 rounded">
            v{card.version}
          </span>
        </div>
      }
      maxWidth="2xl"
    >
      <div className="space-y-5 -mt-2">
        {/* Tab Navigation */}
        <div className="flex items-center gap-6 border-b border-slate-200 dark:border-slate-800 text-xs font-medium pb-2">
          <button
            onClick={() => setActiveTab('details')}
            className={`pb-2 border-b-2 flex items-center gap-1.5 transition-colors -mb-2.5 ${
              activeTab === 'details'
                ? 'border-brand-500 text-brand-600 dark:text-brand-400 font-bold'
                : 'border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-300'
            }`}
          >
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>Details & Workflow</span>
          </button>
          <button
            onClick={() => setActiveTab('comments')}
            className={`pb-2 border-b-2 flex items-center gap-1.5 transition-colors -mb-2.5 ${
              activeTab === 'comments'
                ? 'border-brand-500 text-brand-600 dark:text-brand-400 font-bold'
                : 'border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-300'
            }`}
          >
            <MessageSquare className="w-3.5 h-3.5" />
            <span>Comments ({card.commentCount})</span>
          </button>
          <button
            onClick={() => setActiveTab('history')}
            className={`pb-2 border-b-2 flex items-center gap-1.5 transition-colors -mb-2.5 ${
              activeTab === 'history'
                ? 'border-brand-500 text-brand-600 dark:text-brand-400 font-bold'
                : 'border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-300'
            }`}
          >
            <History className="w-3.5 h-3.5" />
            <span>Audit History</span>
          </button>
          <button
            onClick={() => setActiveTab('attachments')}
            className={`pb-2 border-b-2 flex items-center gap-1.5 transition-colors -mb-2.5 ${
              activeTab === 'attachments'
                ? 'border-brand-500 text-brand-600 dark:text-brand-400 font-bold'
                : 'border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-300'
            }`}
          >
            <Paperclip className="w-3.5 h-3.5" />
            <span>Attachments ({attachments.length})</span>
          </button>
          <button
            onClick={() => setActiveTab('development')}
            className={`pb-2 border-b-2 flex items-center gap-1.5 transition-colors -mb-2.5 ${
              activeTab === 'development'
                ? 'border-brand-500 text-brand-600 dark:text-brand-400 font-bold'
                : 'border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-300'
            }`}
          >
            <GitBranch className="w-3.5 h-3.5 text-emerald-500" />
            <span>Code & PRs ({(card.commitCount || 0) + (card.branchCount || 0) + (card.prCount || 0)})</span>
          </button>
        </div>

        {/* Modal Scrollable Content */}
        <div className="max-h-[60vh] overflow-y-auto space-y-4 pr-1">
          {activeTab === 'details' && (
            <>
              {/* Title */}
              <div>
                <h2 className="text-base font-bold text-slate-900 dark:text-white leading-snug">
                  {card.title}
                </h2>
              </div>

              {/* Status Transition Control (ADR-07 FSM) */}
              <div className="p-4 rounded-xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200 dark:border-slate-800 space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider">
                    Workflow State Machine (FSM)
                  </span>
                  <span className="text-[10px] font-mono text-slate-400">
                    JPA Optimistic Lock: Version {card.version}
                  </span>
                </div>

                <div className="flex flex-wrap items-center gap-2">
                  <span className="text-xs text-slate-500">Current Status:</span>
                  <span className="text-xs font-bold px-2 py-0.5 rounded bg-brand-500/10 text-brand-600 dark:text-brand-400 border border-brand-500/30">
                    {card.status}
                  </span>
                  <ArrowRight className="w-3.5 h-3.5 text-slate-400" />
                  
                  {/* Transition Select */}
                  <select
                    disabled={isTransitioning}
                    value={card.status}
                    onChange={(e) => handleTransition(e.target.value as IssueStatus)}
                    className="px-2.5 py-1 rounded-lg text-xs bg-white dark:bg-slate-900 border border-slate-300 dark:border-slate-700 text-slate-900 dark:text-slate-100 font-medium focus:ring-2 focus:ring-brand-500 cursor-pointer disabled:opacity-50"
                  >
                    {statusOptions.map((st) => (
                      <option key={st} value={st}>
                        {st === card.status ? `(Current) ${st}` : `Transition to ${st}`}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Conflict simulation helper button */}
                <div className="pt-2 border-t border-slate-200/80 dark:border-slate-700/80 flex items-center justify-between">
                  <span className="text-[11px] text-slate-400">
                    Test concurrency rollback:
                  </span>
                  <button
                    onClick={() => handleTransition(card.status === 'DONE' ? 'IN_PROGRESS' : 'DONE', true)}
                    disabled={isTransitioning}
                    className="text-[11px] px-2.5 py-1 rounded bg-rose-500/10 text-rose-600 dark:text-rose-400 border border-rose-500/30 hover:bg-rose-500/20 font-medium flex items-center gap-1 transition-colors"
                  >
                    <AlertTriangle className="w-3 h-3" />
                    <span>Simulate Stale Version Conflict (409)</span>
                  </button>
                </div>
              </div>

              {/* Meta Grid */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
                <div className="p-3 rounded-lg bg-slate-50 dark:bg-slate-800/40 border border-slate-200 dark:border-slate-800">
                  <span className="text-slate-400 block text-[10px] uppercase font-bold mb-1">Priority</span>
                  <span className="font-semibold text-slate-800 dark:text-slate-200">{card.priority}</span>
                </div>
                <div className="p-3 rounded-lg bg-slate-50 dark:bg-slate-800/40 border border-slate-200 dark:border-slate-800">
                  <span className="text-slate-400 block text-[10px] uppercase font-bold mb-1">Type</span>
                  <span className="font-semibold text-slate-800 dark:text-slate-200">{card.issueType}</span>
                </div>
                <div className="p-3 rounded-lg bg-slate-50 dark:bg-slate-800/40 border border-slate-200 dark:border-slate-800">
                  <span className="text-slate-400 block text-[10px] uppercase font-bold mb-1">Story Points</span>
                  <span className="font-semibold font-mono text-slate-800 dark:text-slate-200">{card.storyPoints || '—'}</span>
                </div>
                <div className="p-3 rounded-lg bg-slate-50 dark:bg-slate-800/40 border border-slate-200 dark:border-slate-800">
                  <span className="text-slate-400 block text-[10px] uppercase font-bold mb-1">Assignee</span>
                  <span className="font-semibold text-slate-800 dark:text-slate-200 truncate block">
                    {card.assigneeName || 'Unassigned'}
                  </span>
                </div>
              </div>

              {/* Created & Updated dates */}
              <div className="flex items-center justify-between text-[11px] text-slate-400 pt-2 border-t border-slate-100 dark:border-slate-800/80">
                <span>Created {formatDate(card.createdAt)}</span>
                <span>Last updated {formatTime(card.updatedAt)}</span>
              </div>
            </>
          )}

          {/* Comments Tab */}
          {activeTab === 'comments' && (
            <div className="space-y-4">
              {commentsLoading ? (
                <div className="text-center py-8 text-xs text-slate-400">Loading comments...</div>
              ) : comments.length === 0 ? (
                <div className="text-center py-8 text-xs text-slate-400">
                  No comments yet. Start the discussion below.
                </div>
              ) : (
                <div className="space-y-3">
                  {comments.map((c) => (
                    <div
                      key={c.id}
                      className="p-3.5 rounded-xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200 dark:border-slate-800 space-y-1"
                    >
                      <div className="flex items-center justify-between text-[11px]">
                        <span className="font-semibold text-slate-800 dark:text-slate-200">
                          {c.authorName || 'Team Member'}
                        </span>
                        <span className="text-slate-400 font-mono">
                          {formatTime(c.createdAt)}
                        </span>
                      </div>
                      <p className="text-xs text-slate-700 dark:text-slate-300 whitespace-pre-wrap">{c.body}</p>
                    </div>
                  ))}
                </div>
              )}

              {/* New Comment Input */}
              <form onSubmit={handleAddComment} className="pt-2 flex gap-2">
                <input
                  type="text"
                  value={newCommentText}
                  onChange={(e) => setNewCommentText(e.target.value)}
                  placeholder="Write a comment..."
                  className="flex-1 px-3 py-2 rounded-lg text-xs bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 focus:outline-none focus:ring-2 focus:ring-brand-500 text-slate-900 dark:text-slate-100 placeholder-slate-400"
                />
                <Button
                  type="submit"
                  size="sm"
                  disabled={!newCommentText.trim()}
                  leftIcon={<Send className="w-3.5 h-3.5" />}
                >
                  Send
                </Button>
              </form>
            </div>
          )}

          {/* Audit History Tab */}
          {activeTab === 'history' && (
            <div className="space-y-3">
              {historyLoading ? (
                <div className="text-center py-8 text-xs text-slate-400">Loading audit history...</div>
              ) : auditHistory.length === 0 ? (
                <div className="text-center py-8 text-xs text-slate-400">
                  No audit events recorded yet.
                </div>
              ) : (
                <div className="relative pl-4 space-y-4 before:absolute before:left-1.5 before:top-2 before:bottom-2 before:w-0.5 before:bg-slate-200 dark:before:bg-slate-800">
                  {auditHistory.map((item) => (
                    <div key={item.id} className="relative text-xs space-y-0.5">
                      <span className="absolute -left-4 top-1 w-2.5 h-2.5 rounded-full bg-brand-500 ring-4 ring-white dark:ring-slate-900" />
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-slate-800 dark:text-slate-200">
                          {item.action}
                        </span>
                        <span className="text-[10px] text-slate-400 font-mono">
                          {formatTime(item.createdAt)}
                        </span>
                      </div>
                      {item.afterPayload && (
                        <pre className="text-[10px] font-mono p-2 rounded bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 overflow-x-auto">
                          {item.afterPayload}
                        </pre>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Attachments Tab */}
          {activeTab === 'attachments' && (
            <div className="space-y-4">
              {/* Dropzone / Upload Box */}
              <div
                onDragOver={(e) => {
                  e.preventDefault();
                  setIsDragOver(true);
                }}
                onDragLeave={() => setIsDragOver(false)}
                onDrop={(e) => {
                  e.preventDefault();
                  setIsDragOver(false);
                  if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
                    handleFileUpload(e.dataTransfer.files[0]);
                  }
                }}
                className={`p-6 border-2 border-dashed rounded-xl flex flex-col items-center justify-center text-center transition-colors ${
                  isDragOver
                    ? 'border-brand-500 bg-brand-500/5'
                    : 'border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/40 hover:border-slate-300 dark:hover:border-slate-700'
                }`}
              >
                <UploadCloud className="w-8 h-8 text-slate-400 dark:text-slate-500 mb-2" />
                <p className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                  Drag and drop files to attach, or{' '}
                  <button
                    type="button"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={isUploading}
                    className="text-brand-600 dark:text-brand-400 hover:underline font-bold"
                  >
                    browse
                  </button>
                </p>
                <p className="text-[10px] text-slate-400 mt-1">
                  Supported: PNG, JPEG, GIF, PDF, ZIP, TXT (Max 10 MB). Verified via magic bytes.
                </p>
                <input
                  ref={fileInputRef}
                  type="file"
                  className="hidden"
                  onChange={(e) => {
                    if (e.target.files && e.target.files.length > 0) {
                      handleFileUpload(e.target.files[0]);
                    }
                  }}
                />
                {isUploading && (
                  <p className="text-xs text-brand-600 dark:text-brand-400 font-medium mt-2 animate-pulse">
                    Uploading & verifying magic bytes...
                  </p>
                )}
              </div>

              {/* Attachments List */}
              {attachmentsLoading ? (
                <div className="text-center py-6 text-xs text-slate-400">Loading attachments...</div>
              ) : attachments.length === 0 ? (
                <div className="text-center py-6 text-xs text-slate-400">
                  No attachments yet. Upload documents, screenshots, or logs above.
                </div>
              ) : (
                <div className="space-y-2">
                  {attachments.map((file) => (
                    <div
                      key={file.id}
                      className="flex items-center justify-between p-3 rounded-lg border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-800/80 hover:border-slate-300 dark:hover:border-slate-700 transition-colors"
                    >
                      <div className="flex items-center gap-3 min-w-0">
                        <div className="p-2 rounded bg-slate-100 dark:bg-slate-700/50 flex-shrink-0">
                          {getFileIcon(file.contentType)}
                        </div>
                        <div className="min-w-0">
                          <p className="text-xs font-medium text-slate-800 dark:text-slate-200 truncate max-w-[280px]">
                            {file.originalFilename}
                          </p>
                          <div className="flex items-center gap-2 text-[10px] text-slate-400">
                            <span>{formatBytes(file.sizeBytes)}</span>
                            <span>•</span>
                            <span>{formatDate(file.createdAt)}</span>
                            <span>•</span>
                            <span className="font-mono">{file.contentType}</span>
                          </div>
                        </div>
                      </div>

                      <div className="flex items-center gap-1.5 flex-shrink-0">
                        <button
                          type="button"
                          onClick={() => handleDownload(file)}
                          className="p-1.5 rounded hover:bg-slate-100 dark:hover:bg-slate-700 text-slate-500 hover:text-slate-800 dark:hover:text-slate-200 transition-colors"
                          title="Download attachment"
                        >
                          <Download className="w-3.5 h-3.5" />
                        </button>
                        <button
                          type="button"
                          onClick={() => handleDeleteAttachment(file.id)}
                          className="p-1.5 rounded hover:bg-red-50 dark:hover:bg-red-950/30 text-slate-400 hover:text-red-600 dark:hover:text-red-400 transition-colors"
                          title="Delete attachment"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {activeTab === 'development' && (
            <IssueDevelopmentPanel
              issueId={card.id}
              issueKey={card.formattedKey}
              onErrorToast={onErrorToast}
              onLinksChanged={() => {
                queryClient.invalidateQueries({ queryKey: ['board-view'] });
              }}
            />
          )}
        </div>
      </div>
    </Modal>
  );
};
