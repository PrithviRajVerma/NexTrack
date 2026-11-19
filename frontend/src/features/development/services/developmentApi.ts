import { apiClient } from '@/lib/axios';
import { ENDPOINTS } from '@/services/api/endpoints';
import {
  IssueDevelopmentResponse,
  ManualLinkPayload,
  DevelopmentCandidate,
  ProjectDevelopmentOverview,
  ConnectedRepository,
} from '../types/development.types';

export const initialMockDevelopment: IssueDevelopmentResponse = {
  issueId: '11111111-0000-0000-0000-000000000001',
  branches: [
    {
      id: 'b-1',
      branchId: 'b-1',
      name: 'feature/NT-109-k6-traceability-benchmarks',
      lastCommitSha: 'a3f120c',
      repositoryId: 'repo-1',
      repositoryName: 'nextrack/platform-core',
      linkType: 'AUTO_EXTRACTED',
      linkedAt: new Date(Date.now() - 3600000 * 2).toISOString(),
    },
  ],
  commits: [
    {
      id: 'c-1',
      commitId: 'c-1',
      commitSha: 'a3f120c15efd490bb8a1001234567890abcdef12',
      message: 'NT-109: Implement multi-threaded k6 load scenarios for trace linking',
      authorName: 'Marcus Vance',
      authorEmail: 'marcus@nextrack.io',
      commitUrl: 'https://github.com/nextrack/platform-core/commit/a3f120c15efd490bb8a1001234567890abcdef12',
      committedAt: new Date(Date.now() - 3600000 * 3).toISOString(),
      repositoryId: 'repo-1',
      repositoryName: 'nextrack/platform-core',
      linkType: 'AUTO_EXTRACTED',
      linkedAt: new Date(Date.now() - 3600000 * 3).toISOString(),
    },
    {
      id: 'c-2',
      commitId: 'c-2',
      commitSha: 'b4e231d9980fae12019827364510293847561029',
      message: 'Add fallback parsing for manual commit linking',
      authorName: 'Sarah Chen',
      authorEmail: 'sarah@nextrack.io',
      commitUrl: 'https://github.com/nextrack/platform-core/commit/b4e231d9980fae12019827364510293847561029',
      committedAt: new Date(Date.now() - 3600000 * 1).toISOString(),
      repositoryId: 'repo-1',
      repositoryName: 'nextrack/platform-core',
      linkType: 'MANUAL',
      linkedAt: new Date(Date.now() - 3600000 * 1).toISOString(),
      linkedByName: 'Sarah Chen',
    },
  ],
  pullRequests: [
    {
      id: 'pr-1',
      pullRequestId: 'pr-1',
      providerPrId: 1042,
      prNumber: 42,
      title: 'feat(traceability): [NT-109] High-throughput GitHub webhook ingestion & auto-link',
      description: 'Resolves NT-109. Introduces database migration V010, composite key join tables, and regex auto-linker.',
      sourceBranch: 'feature/NT-109-k6-traceability-benchmarks',
      targetBranch: 'main',
      prUrl: 'https://github.com/nextrack/platform-core/pull/42',
      status: 'OPEN',
      reviewStatus: 'APPROVED',
      isDraft: false,
      repositoryId: 'repo-1',
      repositoryName: 'nextrack/platform-core',
      linkType: 'AUTO_EXTRACTED',
      linkedAt: new Date(Date.now() - 3600000 * 2).toISOString(),
    },
  ],
  totalLinks: 4,
};

export const developmentApi = {
  /**
   * Fetches all linked branches, commits, and pull requests for a given issue.
   */
  async fetchIssueDevelopment(issueId: string): Promise<IssueDevelopmentResponse> {
    try {
      const response = await apiClient.get<IssueDevelopmentResponse>(
        ENDPOINTS.DEVELOPMENT.ISSUE_DEVELOPMENT(issueId)
      );
      return response.data;
    } catch (err) {
      console.warn(`[developmentApi] Backend unavailable, returning mock development for ${issueId}`, err);
      return {
        ...initialMockDevelopment,
        issueId,
      };
    }
  },

  /**
   * Manually links a branch, commit, or PR to an issue.
   */
  async linkArtifact(issueId: string, payload: ManualLinkPayload): Promise<void> {
    await apiClient.post(ENDPOINTS.DEVELOPMENT.ISSUE_LINKS(issueId), payload);
  },

  /**
   * Unlinks an artifact from an issue.
   */
  async unlinkArtifact(issueId: string, targetId: string): Promise<void> {
    await apiClient.delete(ENDPOINTS.DEVELOPMENT.ISSUE_LINK_DETAIL(issueId, targetId));
  },

  /**
   * Searches unlinked candidates in connected repositories.
   */
  async searchCandidates(
    issueId: string,
    artifactType: 'BRANCH' | 'COMMIT' | 'PULL_REQUEST',
    query?: string
  ): Promise<DevelopmentCandidate[]> {
    try {
      const response = await apiClient.get<DevelopmentCandidate[]>(
        ENDPOINTS.DEVELOPMENT.ISSUE_CANDIDATES(issueId),
        {
          params: {
            type: artifactType,
            search: query || undefined,
          },
        }
      );
      return response.data;
    } catch (err) {
      console.warn(`[developmentApi] searchCandidates failed for issue ${issueId}`, err);
      return [];
    }
  },

  /**
   * Fetches high-level project development overview.
   */
  async fetchProjectDevelopmentOverview(projectId: string): Promise<ProjectDevelopmentOverview> {
    try {
      const response = await apiClient.get<ProjectDevelopmentOverview>(
        ENDPOINTS.DEVELOPMENT.PROJECT_OVERVIEW(projectId)
      );
      return response.data;
    } catch (err) {
      console.warn(`[developmentApi] fetchProjectDevelopmentOverview failed for project ${projectId}`, err);
      return {
        connectedRepositories: [
          {
            id: 'repo-1',
            installationId: 'inst-1',
            provider: 'GITHUB',
            externalRepoId: '987654',
            name: 'platform-core',
            fullName: 'nextrack/platform-core',
            htmlUrl: 'https://github.com/nextrack/platform-core',
            defaultBranch: 'main',
            isActive: true,
            lastSyncedAt: new Date().toISOString(),
          },
        ],
        recentCommits: initialMockDevelopment.commits,
        recentPullRequests: initialMockDevelopment.pullRequests,
        recentBranches: initialMockDevelopment.branches,
        summary: {
          totalRepositories: 1,
          totalBranches: 1,
          totalCommits: 2,
          totalPullRequests: 1,
          openPullRequests: 1,
          mergedPullRequests: 0,
        },
      };
    }
  },

  /**
   * Lists repositories linked to a project.
   */
  async listProjectRepositories(projectId: string): Promise<ConnectedRepository[]> {
    const response = await apiClient.get<ConnectedRepository[]>(
      ENDPOINTS.DEVELOPMENT.PROJECT_REPOSITORIES(projectId)
    );
    return response.data;
  },

  /**
   * Links a connected repository to a project.
   */
  async linkRepositoryToProject(projectId: string, repositoryId: string): Promise<void> {
    await apiClient.post(ENDPOINTS.DEVELOPMENT.PROJECT_LINK_REPO(projectId), {
      repositoryId,
    });
  },

  /**
   * Unlinks a repository from a project.
   */
  async unlinkRepositoryFromProject(projectId: string, repositoryId: string): Promise<void> {
    await apiClient.delete(ENDPOINTS.DEVELOPMENT.PROJECT_UNLINK_REPO(projectId, repositoryId));
  },
};
