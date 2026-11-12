import { apiClient } from '@/lib/axios';
import { ENDPOINTS } from '@/services/api/endpoints';
import { NotificationListResponse, NotificationItem } from '../types/notification.types';

export const notificationApi = {
  getNotifications: async (page = 0, size = 20): Promise<NotificationListResponse> => {
    const response = await apiClient.get<NotificationListResponse>(ENDPOINTS.NOTIFICATIONS.BASE, {
      params: { page, size },
    });
    return response.data;
  },

  markAsRead: async (notificationId: string): Promise<NotificationItem> => {
    const response = await apiClient.patch<NotificationItem>(
      ENDPOINTS.NOTIFICATIONS.MARK_READ(notificationId)
    );
    return response.data;
  },

  markAllAsRead: async (): Promise<void> => {
    await apiClient.patch(ENDPOINTS.NOTIFICATIONS.MARK_ALL_READ);
  },
};
