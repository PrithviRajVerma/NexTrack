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
    ATTACHMENTS: (issueId: string) => `/issues/${issueId}/attachments`,
    ATTACHMENT_DETAIL: (issueId: string, attachmentId: string) => `/issues/${issueId}/attachments/${attachmentId}`,
  },
  DEVELOPMENT: {
    ISSUE_DEVELOPMENT: (issueId: string) => `/issues/${issueId}/development`,
    ISSUE_LINKS: (issueId: string) => `/issues/${issueId}/development/links`,
    ISSUE_LINK_DETAIL: (issueId: string, targetId: string) => `/issues/${issueId}/development/links/${targetId}`,
    ISSUE_CANDIDATES: (issueId: string) => `/issues/${issueId}/development/candidates`,
    PROJECT_OVERVIEW: (projectId: string) => `/projects/${projectId}/development`,
    PROJECT_REPOSITORIES: (projectId: string) => `/projects/${projectId}/repositories`,
    PROJECT_LINK_REPO: (projectId: string) => `/projects/${projectId}/repositories/link`,
    PROJECT_UNLINK_REPO: (projectId: string, repositoryId: string) => `/projects/${projectId}/repositories/${repositoryId}`,
  },
  NOTIFICATIONS: {
    BASE: '/notifications',
    MARK_READ: (id: string) => `/notifications/${id}/read`,
    MARK_ALL_READ: '/notifications/read-all',
  },
  HEALTH: '/actuator/health',
} as const;
