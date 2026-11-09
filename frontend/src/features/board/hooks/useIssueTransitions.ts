import { useMutation, useQueryClient } from '@tanstack/react-query';
import { boardApi } from '../services/boardApi';
import { BoardCard, BoardView, IssueStatus } from '../types/board.types';
import { ApiError } from '@/services/api/interceptors';

interface TransitionVariables {
  card: BoardCard;
  targetStatus: IssueStatus;
  triggerConflict?: boolean;
}

export function useIssueTransitions(
  projectId: string,
  onConflict: (err: ApiError, card: BoardCard) => void,
  onSuccessToast?: (card: BoardCard) => void
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ card, targetStatus, triggerConflict }: TransitionVariables) => {
      const sendVersion = triggerConflict ? Math.max(0, card.version - 1) : card.version;
      return await boardApi.transitionIssue(card.id, {
        targetStatus,
        version: sendVersion,
      });
    },
    onMutate: async ({ card, targetStatus }) => {
      await queryClient.cancelQueries({ queryKey: ['board', projectId] });
      const previousBoard = queryClient.getQueryData<BoardView>(['board', projectId]);

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
        if (nextColumns[sourceCol]) {
          nextColumns[sourceCol] = nextColumns[sourceCol].filter((c) => c.id !== card.id);
        }
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
      if (context?.previousBoard) {
        queryClient.setQueryData(['board', projectId], context.previousBoard);
      }
      if (context?.card) {
        onConflict(err, context.card);
      }
    },
    onSuccess: (updatedCard) => {
      onSuccessToast?.(updatedCard);
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['board', projectId] });
    },
  });
}
