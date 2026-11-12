export const ENDPOINTS = {
  AUTH: {
    LOGIN: '/auth/login',
    REGISTER: '/auth/register',
    REFRESH: '/auth/refresh',
    LOGOUT: '/auth/logout',
  },
  PROJECTS: {
    BASE: '/projects',
    DETAIL: (projectId: string) => `/projects/${projectId}`,
    BOARD: (projectId: string) => `/projects/${projectId}/board`,
    ISSUES: (projectId: string) => `/projects/${projectId}/issues`,
  },
  ISSUES: {
    DETAIL: (issueId: string) => `/issues/${issueId}`,
    TRANSITIONS: (issueId: string) => `/issues/${issueId}/transitions`,
    COMMENTS: (issueId: string) => `/issues/${issueId}/comments`,
    HISTORY: (issueId: string) => `/issues/${issueId}/history`,
  },
  NOTIFICATIONS: {
    BASE: '/notifications',
    MARK_READ: (id: string) => `/notifications/${id}/read`,
    MARK_ALL_READ: '/notifications/read-all',
  },
  HEALTH: '/actuator/health',
} as const;
