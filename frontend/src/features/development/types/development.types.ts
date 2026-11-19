export type ArtifactType = 'BRANCH' | 'COMMIT' | 'PULL_REQUEST' | 'PR';
export type LinkType = 'AUTO_EXTRACTED' | 'MANUAL' | 'CONNECTED';
export type PullRequestStatus = 'OPEN' | 'MERGED' | 'CLOSED';

export interface LinkedBranch {
  id: string;
  branchId: string;
  name: string;
  lastCommitSha?: string | null;
  repositoryId: string;
  repositoryName: string;
  linkType: LinkType;
  linkedAt: string;
  linkedByName?: string | null;
}

export interface LinkedCommit {
  id: string;
  commitId: string;
  commitSha: string;
  message: string;
  authorName: string;
  authorEmail: string;
  commitUrl: string;
  committedAt: string;
  repositoryId: string;
  repositoryName: string;
  linkType: LinkType;
  linkedAt: string;
  linkedByName?: string | null;
}

export interface LinkedPullRequest {
  id: string;
  pullRequestId: string;
  providerPrId: number;
  prNumber: number;
  title: string;
  description: string;
  sourceBranch: string;
  targetBranch: string;
  prUrl: string;
  status: PullRequestStatus;
  reviewStatus: string;
  isDraft: boolean;
  mergedAt?: string | null;
  repositoryId: string;
  repositoryName: string;
  linkType: LinkType;
  linkedAt: string;
  linkedByName?: string | null;
}

export interface IssueDevelopmentResponse {
  issueId: string;
  branches: LinkedBranch[];
  commits: LinkedCommit[];
  pullRequests: LinkedPullRequest[];
  totalLinks: number;
}

export interface ManualLinkPayload {
  artifactType: 'BRANCH' | 'COMMIT' | 'PULL_REQUEST';
  artifactId?: string | null;
  identifier?: string | null;
  repositoryId?: string | null;
}

export interface DevelopmentCandidate {
  id: string;
  artifactType: ArtifactType;
  identifier: string;
  title: string;
  subtitle?: string | null;
  url?: string | null;
  timestamp: string;
  repositoryId: string;
  repositoryName: string;
}

export interface ConnectedRepository {
  id: string;
  installationId: string;
  provider: string;
  externalRepoId: string;
  name: string;
  fullName: string;
  htmlUrl: string;
  defaultBranch: string;
  isActive: boolean;
  lastSyncedAt?: string | null;
}

export interface DevelopmentSummary {
  totalRepositories: number;
  totalBranches: number;
  totalCommits: number;
  totalPullRequests: number;
  openPullRequests: number;
  mergedPullRequests: number;
}

export interface ProjectDevelopmentOverview {
  connectedRepositories: ConnectedRepository[];
  recentCommits: LinkedCommit[];
  recentPullRequests: LinkedPullRequest[];
  recentBranches: LinkedBranch[];
  summary: DevelopmentSummary;
}
