import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { boardApi } from '../services/boardApi';
import { BoardView, CreateIssuePayload, IssueStatus } from '../types/board.types';

export function useBoard(projectId: string) {
  const queryClient = useQueryClient();

  const boardQuery = useQuery<BoardView>({
    queryKey: ['board', projectId],
    queryFn: () => boardApi.fetchBoardView(projectId),
    staleTime: 5000,
  });

  const createIssueMutation = useMutation({
    mutationFn: async ({ payload, initialStatus }: { payload: CreateIssuePayload; initialStatus: IssueStatus }) => {
      const created = await boardApi.createIssue(projectId, payload);
      if (initialStatus !== 'BACKLOG' && created.id) {
        await boardApi.transitionIssue(created.id, {
          targetStatus: initialStatus,
          version: created.version || 0,
        });
      }
      return created;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['board', projectId] });
    },
  });

  return {
    board: boardQuery.data,
    isLoading: boardQuery.isLoading,
    isError: boardQuery.isError,
    refetch: boardQuery.refetch,
    createIssue: createIssueMutation.mutateAsync,
    isCreating: createIssueMutation.isPending,
  };
}
