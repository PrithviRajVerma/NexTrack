export const APP_CONSTANTS = {
  DEFAULT_PROJECT_ID: '00000000-0000-0000-0000-000000000001',
  DEFAULT_PROJECT_KEY: 'NT',
  DEFAULT_PROJECT_NAME: 'NexTrack Core',
  THEME_KEY: 'nextrack_theme_mode',
  TOKEN_KEY: 'nextrack_auth_token',
} as const;

export const COLUMN_DEFINITIONS = [
  { status: 'BACKLOG', title: 'Backlog', dotColor: 'bg-slate-400 dark:bg-slate-500' },
  { status: 'TO_DO', title: 'To Do', dotColor: 'bg-blue-500' },
  { status: 'IN_PROGRESS', title: 'In Progress', dotColor: 'bg-amber-500 animate-pulse' },
  { status: 'IN_REVIEW', title: 'In Review', dotColor: 'bg-purple-500' },
  { status: 'DONE', title: 'Done', dotColor: 'bg-emerald-500' },
] as const;
