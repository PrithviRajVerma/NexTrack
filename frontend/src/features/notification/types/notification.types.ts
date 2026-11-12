export interface NotificationPayload {
  recipientId?: string;
  issueId?: string;
  issueNumber?: number;
  formattedKey?: string;
  title?: string;
  commentId?: string;
  fromStatus?: string;
  toStatus?: string;
  actorId?: string;
  dedupKey?: string;
  [key: string]: unknown;
}

export interface NotificationItem {
  id: string;
  eventType: 'ISSUE_ASSIGNED' | 'ISSUE_STATUS_CHANGED' | 'ISSUE_COMMENTED' | string;
  payload: string;
  isRead: boolean;
  readAt: string | null;
  createdAt: string;
}

export interface NotificationListResponse {
  unreadCount: number;
  notifications: NotificationItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
