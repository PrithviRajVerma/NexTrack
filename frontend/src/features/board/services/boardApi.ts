import { apiClient } from '@/lib/axios';
import { ENDPOINTS } from '@/services/api/endpoints';
import { 
  BoardCard, 
  BoardView, 
  CreateIssuePayload, 
  TransitionPayload, 
  IssueComment, 
  AuditEvent 
} from '../types/board.types';

// High-fidelity fallback board state for offline demo
export const initialMockBoard: BoardView = {
  projectId: '00000000-0000-0000-0000-000000000001',
  projectKey: 'NT',
  totalIssues: 7,
  columns: {
    BACKLOG: [
      {
        id: '11111111-0000-0000-0000-000000000001',
        projectId: '00000000-0000-0000-0000-000000000001',
        projectKey: 'NT',
        issueNumber: 109,
        formattedKey: 'NT-109',
        title: 'Configure k6 benchmark harness for issue-to-code traceability load tests',
        status: 'BACKLOG',
        priority: 'MEDIUM',
        issueType: 'TASK',
        assigneeName: 'Marcus Vance',
        assigneeEmail: 'marcus@nextrack.io',
        storyPoints: 5,
        version: 0,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        commentCount: 2,
        attachmentCount: 1,
        prCount: 0,
        commitCount: 3,
        branchCount: 1,
      },
      {
        id: '11111111-0000-0000-0000-000000000002',
        projectId: '00000000-0000-0000-0000-000000000001',
        projectKey: 'NT',
        issueNumber: 112,
        formattedKey: 'NT-112',
        title: 'Support Bitbucket & GitLab Webhook payload normalizers via Provider SPI port',
        status: 'BACKLOG',
        priority: 'LOW',
        issueType: 'STORY',
        assigneeName: null,
        storyPoints: 8,
        version: 0,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        commentCount: 0,
        attachmentCount: 0,
        prCount: 0,
        commitCount: 0,
        branchCount: 0,
      },
    ],
    TO_DO: [
      {
        id: '11111111-0000-0000-0000-000000000003',
        projectId: '00000000-0000-0000-0000-000000000001',
        projectKey: 'NT',
        issueNumber: 104,
        formattedKey: 'NT-104',
        title: 'Implement project-key uniqueness constraint UNIQUE(workspace_id, project_key)',
        status: 'TO_DO',
        priority: 'HIGH',
        issueType: 'TASK',
        assigneeName: 'PrithviRaj Verma',
        assigneeEmail: 'prithvi@nextrack.io',
        storyPoints: 3,
        version: 1,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        commentCount: 4,
        attachmentCount: 0,
        prCount: 1,
        commitCount: 2,
        branchCount: 1,
      },
      {
        id: '11111111-0000-0000-0000-000000000004',
        projectId: '00000000-0000-0000-0000-000000000001',
        projectKey: 'NT',
        issueNumber: 107,
        formattedKey: 'NT-107',
        title: 'Enforce streaming max body size limit (2MB) before buffer allocation',
        status: 'TO_DO',
        priority: 'URGENT',
        issueType: 'BUG',
        assigneeName: 'Alex Chen',
        assigneeEmail: 'alex@nextrack.io',
        storyPoints: 2,
        version: 0,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        commentCount: 1,
        attachmentCount: 0,
        prCount: 0,
        commitCount: 0,
        branchCount: 0,
      },
    ],
    IN_PROGRESS: [
      {
        id: '11111111-0000-0000-0000-000000000005',
        projectId: '00000000-0000-0000-0000-000000000001',
        projectKey: 'NT',
        issueNumber: 101,
        formattedKey: 'NT-101',
        title: 'GitHub Webhook Inbox 2-phase worker with lease ownership token verification',
        status: 'IN_PROGRESS',
        priority: 'URGENT',
        issueType: 'STORY',
        assigneeName: 'PrithviRaj Verma',
        assigneeEmail: 'prithvi@nextrack.io',
        storyPoints: 13,
        version: 2,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        commentCount: 8,
        attachmentCount: 2,
        prCount: 2,
        commitCount: 7,
        branchCount: 1,
      },
      {
        id: '11111111-0000-0000-0000-000000000006',
        projectId: '00000000-0000-0000-0000-000000000001',
        projectKey: 'NT',
        issueNumber: 103,
        formattedKey: 'NT-103',
        title: 'Multi-tenant workspace isolation filter & RS256 token verification middleware',
        status: 'IN_PROGRESS',
        priority: 'HIGH',
        issueType: 'TASK',
        assigneeName: 'Sarah Lin',
        assigneeEmail: 'sarah@nextrack.io',
        storyPoints: 8,
        version: 3,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        commentCount: 5,
        attachmentCount: 0,
        prCount: 1,
        commitCount: 4,
        branchCount: 1,
      },
    ],
    IN_REVIEW: [
      {
        id: '11111111-0000-0000-0000-000000000007',
        projectId: '00000000-0000-0000-0000-000000000001',
        projectKey: 'NT',
        issueNumber: 98,
        formattedKey: 'NT-98',
        title: 'Implement optimistic locking @Version on issue updates mapping to 409 Conflict',
        status: 'IN_REVIEW',
        priority: 'HIGH',
        issueType: 'TASK',
        assigneeName: 'Alex Chen',
        assigneeEmail: 'alex@nextrack.io',
        storyPoints: 5,
        version: 4,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        commentCount: 3,
        attachmentCount: 1,
        prCount: 1,
        commitCount: 2,
        branchCount: 1,
      },
    ],
    DONE: [
      {
        id: '11111111-0000-0000-0000-000000000008',
        projectId: '00000000-0000-0000-0000-000000000001',
        projectKey: 'NT',
        issueNumber: 95,
        formattedKey: 'NT-95',
        title: 'Database schema migration baseline with Flyway and Testcontainers integration',
        status: 'DONE',
        priority: 'MEDIUM',
        issueType: 'TASK',
        assigneeName: 'PrithviRaj Verma',
        assigneeEmail: 'prithvi@nextrack.io',
        storyPoints: 5,
        version: 2,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        commentCount: 6,
        attachmentCount: 0,
        prCount: 1,
        commitCount: 3,
        branchCount: 1,
      },
    ],
    ARCHIVED: [],
  },
};

export const boardApi = {
  fetchBoardView: async (projectId: string): Promise<BoardView> => {
    try {
      const res = await apiClient.get<BoardView>(ENDPOINTS.PROJECTS.BOARD(projectId));
      return res.data;
    } catch (err: any) {
      if (err?.status === 404 || err?.status === 401 || err?.status === 403 || err?.code === 'ERR_NETWORK') {
        return initialMockBoard;
      }
      throw err;
    }
  },

  transitionIssue: async (issueId: string, payload: TransitionPayload): Promise<BoardCard> => {
    const res = await apiClient.post<BoardCard>(ENDPOINTS.ISSUES.TRANSITIONS(issueId), payload);
    return res.data;
  },

  createIssue: async (projectId: string, payload: CreateIssuePayload): Promise<any> => {
    const res = await apiClient.post(ENDPOINTS.PROJECTS.ISSUES(projectId), payload);
    return res.data;
  },

  fetchComments: async (issueId: string): Promise<IssueComment[]> => {
    try {
      const res = await apiClient.get<IssueComment[]>(ENDPOINTS.ISSUES.COMMENTS(issueId));
      return res.data;
    } catch {
      return [];
    }
  },

  addComment: async (issueId: string, body: string): Promise<IssueComment> => {
    const res = await apiClient.post<IssueComment>(ENDPOINTS.ISSUES.COMMENTS(issueId), { body });
    return res.data;
  },

  fetchIssueHistory: async (issueId: string): Promise<AuditEvent[]> => {
    try {
      const res = await apiClient.get<AuditEvent[]>(ENDPOINTS.ISSUES.HISTORY(issueId));
      return res.data;
    } catch {
      return [];
    }
  },
};
