import React from 'react';
import { Bell, Check, CheckCheck, X, UserCheck, ArrowRightCircle, MessageSquare } from 'lucide-react';
import { useNotifications, useMarkNotificationRead, useMarkAllNotificationsRead } from '../hooks/useNotifications';
import { NotificationItem, NotificationPayload } from '../types/notification.types';

interface NotificationDrawerProps {
  isOpen: boolean;
  onClose: () => void;
}

export const NotificationDrawer: React.FC<NotificationDrawerProps> = ({ isOpen, onClose }) => {
  const { data, isLoading } = useNotifications(0, 30);
  const markReadMutation = useMarkNotificationRead();
  const markAllMutation = useMarkAllNotificationsRead();

  if (!isOpen) return null;

  const parsePayload = (rawPayload: string): NotificationPayload => {
    try {
      return JSON.parse(rawPayload);
    } catch {
      return {};
    }
  };

  const getEventIcon = (eventType: string) => {
    switch (eventType) {
      case 'ISSUE_ASSIGNED':
        return <UserCheck className="w-4 h-4 text-brand-400" />;
      case 'ISSUE_STATUS_CHANGED':
        return <ArrowRightCircle className="w-4 h-4 text-emerald-400" />;
      case 'ISSUE_COMMENTED':
        return <MessageSquare className="w-4 h-4 text-amber-400" />;
      default:
        return <Bell className="w-4 h-4 text-slate-400" />;
    }
  };

  const formatTimestamp = (isoDate: string) => {
    const diffMs = Date.now() - new Date(isoDate).getTime();
    const diffSec = Math.floor(diffMs / 1000);
    if (diffSec < 60) return 'Just now';
    const diffMin = Math.floor(diffSec / 60);
    if (diffMin < 60) return `${diffMin}m ago`;
    const diffHours = Math.floor(diffMin / 60);
    if (diffHours < 24) return `${diffHours}h ago`;
    return `${Math.floor(diffHours / 24)}d ago`;
  };

  const unreadCount = data?.unreadCount ?? 0;
  const notifications = data?.notifications ?? [];

  return (
    <div className="fixed inset-0 z-50 overflow-hidden bg-black/40 backdrop-blur-xs flex justify-end">
      <div 
        className="w-full max-w-md bg-white dark:bg-[#11141D] border-l border-slate-200 dark:border-slate-800 shadow-2xl h-full flex flex-col animate-in slide-in-from-right duration-200"
        role="dialog"
        aria-label="Notification Center"
      >
        {/* Header */}
        <div className="px-5 py-4 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-lg bg-brand-500/10 text-brand-500">
              <Bell className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-sm font-semibold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                Notifications
                {unreadCount > 0 && (
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-mono font-bold bg-brand-500 text-white">
                    {unreadCount}
                  </span>
                )}
              </h2>
              <p className="text-[11px] text-slate-500 dark:text-slate-400">
                Transactional Outbox events & domain alerts
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {unreadCount > 0 && (
              <button
                onClick={() => markAllMutation.mutate()}
                disabled={markAllMutation.isPending}
                className="flex items-center gap-1 px-2.5 py-1 text-xs font-medium text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-colors"
                title="Mark all as read"
              >
                <CheckCheck className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">Mark all</span>
              </button>
            )}
            <button
              onClick={onClose}
              className="p-1.5 text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Content List */}
        <div className="flex-1 overflow-y-auto divide-y divide-slate-100 dark:divide-slate-800/60">
          {isLoading ? (
            <div className="p-8 text-center text-xs text-slate-400">
              Loading notifications...
            </div>
          ) : notifications.length === 0 ? (
            <div className="p-12 text-center">
              <div className="w-10 h-10 rounded-full bg-slate-100 dark:bg-slate-800/80 flex items-center justify-center mx-auto mb-3 text-slate-400">
                <Bell className="w-5 h-5 opacity-60" />
              </div>
              <p className="text-xs font-medium text-slate-600 dark:text-slate-400">
                No notifications yet
              </p>
              <p className="text-[11px] text-slate-400 dark:text-slate-500 mt-1">
                You'll receive alerts here when issues are assigned or transitioned.
              </p>
            </div>
          ) : (
            notifications.map((item: NotificationItem) => {
              const payload = parsePayload(item.payload);
              return (
                <div
                  key={item.id}
                  className={`p-4 transition-colors flex items-start gap-3 ${
                    item.isRead
                      ? 'bg-transparent opacity-75'
                      : 'bg-brand-500/5 dark:bg-brand-500/[0.03]'
                  }`}
                >
                  <div className="mt-0.5 p-1.5 rounded-lg bg-slate-100 dark:bg-slate-800/80 shrink-0">
                    {getEventIcon(item.eventType)}
                  </div>

                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-1.5 flex-wrap">
                      {payload.formattedKey && (
                        <span className="font-mono text-[10px] font-bold text-brand-600 dark:text-brand-400 bg-brand-500/10 px-1.5 py-0.5 rounded">
                          {payload.formattedKey}
                        </span>
                      )}
                      <span className="text-xs font-medium text-slate-900 dark:text-slate-200 truncate">
                        {item.eventType === 'ISSUE_ASSIGNED' && 'Assigned to you'}
                        {item.eventType === 'ISSUE_STATUS_CHANGED' && `Status changed to ${payload.toStatus ?? ''}`}
                        {item.eventType === 'ISSUE_COMMENTED' && 'New comment'}
                        {!['ISSUE_ASSIGNED', 'ISSUE_STATUS_CHANGED', 'ISSUE_COMMENTED'].includes(item.eventType) && item.eventType}
                      </span>
                    </div>

                    {payload.title && (
                      <p className="text-xs text-slate-600 dark:text-slate-400 mt-1 line-clamp-2">
                        {payload.title}
                      </p>
                    )}

                    <div className="flex items-center gap-3 mt-2 text-[10px] text-slate-400">
                      <span>{formatTimestamp(item.createdAt)}</span>
                      {!item.isRead && (
                        <button
                          onClick={() => markReadMutation.mutate(item.id)}
                          disabled={markReadMutation.isPending}
                          className="text-brand-600 dark:text-brand-400 hover:underline flex items-center gap-1"
                        >
                          <Check className="w-3 h-3" />
                          <span>Mark read</span>
                        </button>
                      )}
                    </div>
                  </div>

                  {!item.isRead && (
                    <span className="w-2 h-2 rounded-full bg-brand-500 shrink-0 mt-2" />
                  )}
                </div>
              );
            })
          )}
        </div>

        {/* Footer info */}
        <div className="px-5 py-3 bg-slate-50 dark:bg-slate-900/60 border-t border-slate-200 dark:border-slate-800 text-[10px] text-slate-400 flex items-center justify-between">
          <span>PostgreSQL Outbox Worker (SKIP LOCKED)</span>
          <span className="font-mono">At-Least-Once</span>
        </div>
      </div>
    </div>
  );
};
