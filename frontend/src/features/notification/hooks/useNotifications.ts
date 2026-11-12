import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { notificationApi } from '../services/notificationApi';

export const NOTIFICATION_QUERY_KEY = ['notifications'];

export const useNotifications = (page = 0, size = 20) => {
  return useQuery({
    queryKey: [...NOTIFICATION_QUERY_KEY, page, size],
    queryFn: () => notificationApi.getNotifications(page, size),
    refetchInterval: 10000, // Poll every 10 seconds for real-time notification badge
  });
};

export const useMarkNotificationRead = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => notificationApi.markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: NOTIFICATION_QUERY_KEY });
    },
  });
};

export const useMarkAllNotificationsRead = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => notificationApi.markAllAsRead(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: NOTIFICATION_QUERY_KEY });
    },
  });
};
