import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { 
  BoardCard, 
  CreateIssuePayload, 
  IssuePriority, 
  IssueStatus, 
  IssueType 
} from '../types/board.types';
import { ToastMessage } from '@/types/global';
import { APP_CONSTANTS, COLUMN_DEFINITIONS } from '@/constants/app.constants';
import { useBoard } from '../hooks/useBoard';
import { useIssueTransitions } from '../hooks/useIssueTransitions';
import { BoardColumn } from './BoardColumn';
import { BoardToolbar } from './BoardToolbar';
import { ConflictToastContainer } from './ConflictToast';
import { CreateIssueModal } from './CreateIssueModal';
import { IssueDetailModal } from './IssueDetailModal';
import { Loader } from '@/components/ui/Loader';
import { ApiError } from '@/services/api/interceptors';

interface KanbanBoardProps {
  projectId?: string;
}

export const KanbanBoard: React.FC<KanbanBoardProps> = ({ 
  projectId = APP_CONSTANTS.DEFAULT_PROJECT_ID 
}) => {
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

  // Board Hook
  const { board, isLoading, createIssue } = useBoard(projectId);

  // Transition Hook
  const transitionMutation = useIssueTransitions(
    projectId,
    (err: ApiError, card: BoardCard) => {
      if (err.status === 409) {
        addToast(
          'Optimistic Concurrency Conflict (409)',
          `Issue was modified by another user concurrently. The card "${card.formattedKey}" has been reverted to its original column.`,
          'warning'
        );
      } else if (err.status === 400) {
        addToast(
          'Workflow State Machine Denied (400)',
          err.message || 'Transition disallowed by finite state machine rules.',
          'error'
        );
      } else {
        addToast('Transition Failed', err.message || 'Could not transition issue. Card rolled back.', 'error');
      }
    },
    (updatedCard: BoardCard) => {
      addToast(
        'Card Transitioned',
        `${updatedCard.formattedKey} moved to ${updatedCard.status} (version ${updatedCard.version})`,
        'success'
      );
    }
  );

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
      const created = await createIssue({ payload, initialStatus });
      addToast(
        'Issue Created',
        `${created.issueKey || 'New Issue'} created in ${initialStatus}`,
        'success'
      );
    } catch {
      addToast('Issue Created (Local)', `Issue created in ${initialStatus}`, 'success');
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

    for (const col of COLUMN_DEFINITIONS) {
      const cardsInCol = board.columns[col.status] || [];
      result[col.status] = cardsInCol.filter((card) => {
        if (query) {
          const matchKey = card.formattedKey.toLowerCase().includes(query);
          const matchTitle = card.title.toLowerCase().includes(query);
          if (!matchKey && !matchTitle) return false;
        }

        if (selectedPriority !== 'ALL' && card.priority !== selectedPriority) {
          return false;
        }

        if (selectedType !== 'ALL' && card.issueType !== selectedType) {
          return false;
        }

        if (onlyWithPRs && card.prCount === 0) {
          return false;
        }

        return true;
      });
    }

    return result;
  }, [board, searchQuery, selectedPriority, selectedType, onlyWithPRs]);

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
            <Loader size="lg" text="Loading board projections..." />
          </div>
        ) : (
          COLUMN_DEFINITIONS.map((col) => (
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
        }}
        onErrorToast={addToast}
      />

      {/* Toast Notifications */}
      <ConflictToastContainer toasts={toasts} onDismiss={dismissToast} />
    </div>
  );
};
