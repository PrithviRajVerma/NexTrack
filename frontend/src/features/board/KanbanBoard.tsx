import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { 
  BoardCard, 
  BoardView, 
  CreateIssuePayload, 
  IssuePriority, 
  IssueStatus, 
  IssueType, 
  ToastMessage 
} from '../../types/board';
import { fetchBoardView, transitionIssue, createIssue, ApiError } from '../../services/api';
import { BoardColumn } from './BoardColumn';
import { BoardToolbar } from './BoardToolbar';
import { ToastContainer } from './ConflictToast';
import { CreateIssueModal } from './CreateIssueModal';
import { IssueDetailModal } from './IssueDetailModal';
import { Loader2 } from 'lucide-react';

interface KanbanBoardProps {
  projectId?: string;
}

const DEFAULT_PROJECT_ID = '00000000-0000-0000-0000-000000000001';

const COLUMNS_CONFIG: { status: IssueStatus; title: string; dotColor: string }[] = [
  { status: 'BACKLOG', title: 'Backlog', dotColor: 'bg-slate-400 dark:bg-slate-500' },
  { status: 'TO_DO', title: 'To Do', dotColor: 'bg-blue-500' },
  { status: 'IN_PROGRESS', title: 'In Progress', dotColor: 'bg-amber-500 animate-pulse' },
  { status: 'IN_REVIEW', title: 'In Review', dotColor: 'bg-purple-500' },
  { status: 'DONE', title: 'Done', dotColor: 'bg-emerald-500' },
];

export const KanbanBoard: React.FC<KanbanBoardProps> = ({ projectId = DEFAULT_PROJECT_ID }) => {
  const queryClient = useQueryClient();

  // Filters State
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedPriority, setSelectedPriority] = useState<IssuePriority | 'ALL'>('ALL');
  const [selectedType, setSelectedType] = useState<IssueType | 'ALL'>('ALL');
  const [onlyWithPRs, setOnlyWithPRs] = useState(false);
  const [simulateConflict, setSimulateConflict] = useState(false);

  // Drag & Drop State
  const [draggedCard, setDraggedCard] = useState<BoardCard | null>(null);

  // Modals & Toasts State
  const [selectedCardForDetail, setSelectedCardForDetail] = useState<BoardCard | null>(null);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [initialColumnForCreate, setInitialColumnForCreate] = useState<IssueStatus>('BACKLOG');
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  // Add toast helper
  const addToast = useCallback((title: string, message: string, type: ToastMessage['type'] = 'info') => {
    const id = Math.random().toString(36).substring(2, 9);
    setToasts((prev) => [...prev, { id, title, message, type }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 5000);
  }, []);

  const dismissToast = (id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  // Keyboard Shortcuts: 'C' for Create Issue, 'Escape' to close modals
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) {
        return;
      }
      if (e.key === 'c' || e.key === 'C') {
        e.preventDefault();
        setInitialColumnForCreate('BACKLOG');
        setIsCreateOpen(true);
      }
      if (e.key === 'Escape') {
        setIsCreateOpen(false);
        setSelectedCardForDetail(null);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  // Board Data Query
  const { 
    data: board, 
    isLoading
  } = useQuery<BoardView>({
    queryKey: ['board', projectId],
    queryFn: () => fetchBoardView(projectId),
    staleTime: 5000,
  });

  // Optimistic Transition Mutation with Rollback on Conflict
  const transitionMutation = useMutation({
    mutationFn: async ({ card, targetStatus, triggerConflict }: { card: BoardCard; targetStatus: IssueStatus; triggerConflict: boolean }) => {
      // If simulateConflict is active, pass a stale version (card.version - 1) to force 409 Conflict
      const sendVersion = triggerConflict ? Math.max(0, card.version - 1) : card.version;
      return await transitionIssue(card.id, {
        targetStatus,
        version: sendVersion,
      });
    },
    onMutate: async ({ card, targetStatus }) => {
      // 1. Cancel in-flight queries
      await queryClient.cancelQueries({ queryKey: ['board', projectId] });

      // 2. Snapshot current state for rollback
      const previousBoard = queryClient.getQueryData<BoardView>(['board', projectId]);

      // 3. Optimistically update local query cache
      if (previousBoard) {
        const sourceCol = card.status;
        const targetCol = targetStatus;

        const updatedCard: BoardCard = {
          ...card,
          status: targetCol,
          version: card.version + 1,
          updatedAt: new Date().toISOString(),
        };

        const nextColumns = { ...previousBoard.columns };

        // Remove from source
        if (nextColumns[sourceCol]) {
          nextColumns[sourceCol] = nextColumns[sourceCol].filter((c) => c.id !== card.id);
        }

        // Add to target
        if (nextColumns[targetCol]) {
          nextColumns[targetCol] = [updatedCard, ...nextColumns[targetCol]];
        } else {
          nextColumns[targetCol] = [updatedCard];
        }

        queryClient.setQueryData<BoardView>(['board', projectId], {
          ...previousBoard,
          columns: nextColumns,
        });
      }

      return { previousBoard, card };
    },
    onError: (err: any, _vars, context) => {
      // Rollback to prior snapshot
      if (context?.previousBoard) {
        queryClient.setQueryData(['board', projectId], context.previousBoard);
      }

      if (err instanceof ApiError && err.status === 409) {
        addToast(
          'Optimistic Concurrency Conflict (409)',
          `Issue was modified by another user concurrently. The card "${context?.card?.formattedKey}" has been reverted to its original column.`,
          'warning'
        );
      } else if (err instanceof ApiError && err.status === 400) {
        addToast(
          'Workflow State Machine Denied (400)',
          err.message || 'Transition disallowed by finite state machine rules.',
          'error'
        );
      } else {
        addToast(
          'Transition Failed',
          err?.message || 'Could not transition issue. Card rolled back.',
          'error'
        );
      }
    },
    onSuccess: (updatedCard) => {
      addToast(
        'Card Transitioned',
        `${updatedCard.formattedKey} moved to ${updatedCard.status} (version ${updatedCard.version})`,
        'success'
      );
    },
    onSettled: () => {
      // Always refetch to sync authoritative database state
      queryClient.invalidateQueries({ queryKey: ['board', projectId] });
    },
  });

  // Handle Drag & Drop
  const handleDragStart = (_e: React.DragEvent, card: BoardCard) => {
    setDraggedCard(card);
  };

  const handleDropOnColumn = (targetStatus: IssueStatus) => {
    if (!draggedCard) return;
    if (draggedCard.status === targetStatus) {
      setDraggedCard(null);
      return;
    }

    transitionMutation.mutate({
      card: draggedCard,
      targetStatus,
      triggerConflict: simulateConflict,
    });

    setDraggedCard(null);
  };

  // Create Issue Handler
  const handleCreateIssue = async (payload: CreateIssuePayload, initialStatus: IssueStatus) => {
    try {
      const created = await createIssue(projectId, payload);
      // If initialStatus is not BACKLOG, transition it
      if (initialStatus !== 'BACKLOG' && created.id) {
        await transitionIssue(created.id, {
          targetStatus: initialStatus,
          version: created.version || 0,
        });
      }

      addToast(
        'Issue Created',
        `${created.issueKey || 'New Issue'} created in ${initialStatus}`,
        'success'
      );
      queryClient.invalidateQueries({ queryKey: ['board', projectId] });
    } catch (err: any) {
      // If backend is unreachable or local mock, add directly to query cache
      console.warn('Backend create failed, updating local state:', err);
      const mockKey = `NT-${Math.floor(Math.random() * 800) + 120}`;
      const newCard: BoardCard = {
        id: Math.random().toString(36).substring(2, 11),
        projectId,
        projectKey: 'NT',
        issueNumber: 999,
        formattedKey: mockKey,
        title: payload.title,
        status: initialStatus,
        priority: payload.priority,
        issueType: payload.issueType,
        storyPoints: payload.storyPoints,
        version: 0,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        commentCount: 0,
        attachmentCount: 0,
        prCount: 0,
        commitCount: 0,
        branchCount: 0,
      };

      if (board) {
        const nextCols = { ...board.columns };
        nextCols[initialStatus] = [newCard, ...(nextCols[initialStatus] || [])];
        queryClient.setQueryData(['board', projectId], {
          ...board,
          totalIssues: board.totalIssues + 1,
          columns: nextCols,
        });
      }

      addToast('Issue Created (Local)', `${mockKey} created in ${initialStatus}`, 'success');
    }
  };

  // Filtered Cards per Column
  const filteredColumns = useMemo(() => {
    if (!board) return {} as Record<IssueStatus, BoardCard[]>;

    const result: Record<IssueStatus, BoardCard[]> = {
      BACKLOG: [],
      TO_DO: [],
      IN_PROGRESS: [],
      IN_REVIEW: [],
      DONE: [],
      ARCHIVED: [],
    };

    const query = searchQuery.trim().toLowerCase();

    for (const col of COLUMNS_CONFIG) {
      const cardsInCol = board.columns[col.status] || [];
      result[col.status] = cardsInCol.filter((card) => {
        // Search filter
        if (query) {
          const matchKey = card.formattedKey.toLowerCase().includes(query);
          const matchTitle = card.title.toLowerCase().includes(query);
          if (!matchKey && !matchTitle) return false;
        }

        // Priority filter
        if (selectedPriority !== 'ALL' && card.priority !== selectedPriority) {
          return false;
        }

        // Type filter
        if (selectedType !== 'ALL' && card.issueType !== selectedType) {
          return false;
        }

        // PR filter
        if (onlyWithPRs && card.prCount === 0) {
          return false;
        }

        return true;
      });
    }

    return result;
  }, [board, searchQuery, selectedPriority, selectedType, onlyWithPRs]);

  // Total visible count calculation
  const totalVisible = useMemo(() => {
    return Object.values(filteredColumns).reduce((acc, cards) => acc + cards.length, 0);
  }, [filteredColumns]);

  const totalAll = board?.totalIssues || 0;

  return (
    <div className="flex-1 flex flex-col min-h-0 bg-slate-50 dark:bg-slate-950">
      {/* Board Toolbar */}
      <BoardToolbar
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        selectedPriority={selectedPriority}
        onPriorityChange={setSelectedPriority}
        selectedType={selectedType}
        onTypeChange={setSelectedType}
        onlyWithPRs={onlyWithPRs}
        onToggleOnlyWithPRs={() => setOnlyWithPRs(!onlyWithPRs)}
        simulateConflict={simulateConflict}
        onToggleSimulateConflict={() => {
          setSimulateConflict(!simulateConflict);
          addToast(
            simulateConflict ? 'Conflict Mode Disabled' : 'Conflict Mode Enabled',
            simulateConflict
              ? 'Normal card movements restored.'
              : 'Next drag & drop will submit a stale version to test 409 Conflict rollback.',
            simulateConflict ? 'info' : 'warning'
          );
        }}
        onOpenCreateModal={() => {
          setInitialColumnForCreate('BACKLOG');
          setIsCreateOpen(true);
        }}
        onResetFilters={() => {
          setSearchQuery('');
          setSelectedPriority('ALL');
          setSelectedType('ALL');
          setOnlyWithPRs(false);
        }}
        totalVisible={totalVisible}
        totalAll={totalAll}
      />

      {/* Main Board View */}
      <div className="flex-1 p-6 overflow-x-auto flex gap-4 items-start select-none">
        {isLoading && !board ? (
          <div className="flex-1 flex flex-col items-center justify-center py-20 text-slate-400 gap-3">
            <Loader2 className="w-8 h-8 animate-spin text-brand-500" />
            <span className="text-xs">Loading board projections...</span>
          </div>
        ) : (
          COLUMNS_CONFIG.map((col) => (
            <BoardColumn
              key={col.status}
              status={col.status}
              title={col.title}
              dotColor={col.dotColor}
              cards={filteredColumns[col.status] || []}
              onCardClick={(card) => setSelectedCardForDetail(card)}
              onDragStart={handleDragStart}
              onDrop={handleDropOnColumn}
              onAddIssueClick={(status) => {
                setInitialColumnForCreate(status);
                setIsCreateOpen(true);
              }}
            />
          ))
        )}
      </div>

      {/* Create Issue Modal */}
      <CreateIssueModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onSubmit={handleCreateIssue}
        initialStatus={initialColumnForCreate}
      />

      {/* Issue Detail Drawer / Modal */}
      <IssueDetailModal
        card={selectedCardForDetail}
        isOpen={!!selectedCardForDetail}
        onClose={() => setSelectedCardForDetail(null)}
        onStatusChanged={(cardId, newStatus, newVersion) => {
          if (selectedCardForDetail && selectedCardForDetail.id === cardId) {
            setSelectedCardForDetail({
              ...selectedCardForDetail,
              status: newStatus,
              version: newVersion,
            });
          }
          queryClient.invalidateQueries({ queryKey: ['board', projectId] });
        }}
        onErrorToast={addToast}
      />

      {/* Toast Notifications */}
      <ToastContainer toasts={toasts} onDismiss={dismissToast} />
    </div>
  );
};
