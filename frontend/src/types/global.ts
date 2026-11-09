export type ThemeMode = 'light' | 'dark';

export interface UserProfile {
  id: string;
  email: string;
  displayName: string;
  avatarUrl?: string | null;
  role?: string;
}

export interface Workspace {
  id: string;
  name: string;
  slug: string;
  ownerUserId: string;
}

export interface Project {
  id: string;
  workspaceId: string;
  name: string;
  projectKey: string;
  description?: string | null;
}

export interface ToastMessage {
  id: string;
  title: string;
  message: string;
  type: 'success' | 'error' | 'warning' | 'info';
}
