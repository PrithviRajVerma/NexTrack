export type IssueStatus = 'BACKLOG' | 'TO_DO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE' | 'ARCHIVED';
export type IssuePriority = 'URGENT' | 'HIGH' | 'MEDIUM' | 'LOW';
export type IssueType = 'TASK' | 'BUG' | 'STORY' | 'EPIC';

export interface BoardCard {
  id: string;
  projectId: string;
  projectKey: string;
  issueNumber: number;
  formattedKey: string;
  title: string;
  status: IssueStatus;
  priority: IssuePriority;
  issueType: IssueType;
  assigneeId?: string | null;
  assigneeName?: string | null;
  assigneeEmail?: string | null;
  dueDate?: string | null;
  storyPoints?: number | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  commentCount: number;
  attachmentCount: number;
  prCount: number;
  commitCount: number;
  branchCount: number;
}

export interface BoardView {
  projectId: string;
  projectKey: string;
  columns: Record<IssueStatus, BoardCard[]>;
  totalIssues: number;
}

export interface CreateIssuePayload {
  title: string;
  description?: string;
  issueType: IssueType;
  priority: IssuePriority;
  assigneeId?: string | null;
  dueDate?: string | null;
  storyPoints?: number | null;
}

export interface TransitionPayload {
  targetStatus: IssueStatus;
  version: number;
}

export interface AuditEvent {
  id: string;
  actorId?: string | null;
  entityType: string;
  entityId: string;
  action: string;
  beforePayload?: string | null;
  afterPayload?: string | null;
  traceId?: string | null;
  createdAt: string;
}

export interface IssueComment {
  id: string;
  issueId: string;
  authorId: string;
  authorName?: string;
  body: string;
  createdAt: string;
  updatedAt: string;
}

export interface ToastMessage {
  id: string;
  title: string;
  message: string;
  type: 'success' | 'error' | 'warning' | 'info';
}
