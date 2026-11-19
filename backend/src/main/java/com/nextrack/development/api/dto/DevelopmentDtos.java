package com.nextrack.development.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class DevelopmentDtos {

    public record CreateInstallationRequest(
            Long installationId,
            String accountName,
            String repositorySelection,
            String permissions
    ) {}

    public record InstallationResponse(
            UUID id,
            UUID workspaceId,
            String provider,
            Long installationId,
            String accountName,
            String repositorySelection,
            Instant createdAt
    ) {}

    public record ConnectRepositoryRequest(
            String externalRepoId,
            String name,
            String fullName,
            String htmlUrl,
            String defaultBranch
    ) {}

    public record ConnectedRepositoryResponse(
            UUID id,
            UUID installationId,
            String provider,
            String externalRepoId,
            String name,
            String fullName,
            String htmlUrl,
            String defaultBranch,
            boolean isActive,
            Instant lastSyncedAt
    ) {}

    public record ProjectRepositoryLinkRequest(
            UUID repositoryId
    ) {}

    public record LinkedBranchResponse(
            UUID id,
            UUID branchId,
            String name,
            String lastCommitSha,
            UUID repositoryId,
            String repositoryName,
            String linkType,
            Instant linkedAt,
            String linkedByName
    ) {}

    public record LinkedCommitResponse(
            UUID id,
            UUID commitId,
            String commitSha,
            String message,
            String authorName,
            String authorEmail,
            String commitUrl,
            Instant committedAt,
            UUID repositoryId,
            String repositoryName,
            String linkType,
            Instant linkedAt,
            String linkedByName
    ) {}

    public record LinkedPullRequestResponse(
            UUID id,
            UUID pullRequestId,
            Long providerPrId,
            Integer prNumber,
            String title,
            String description,
            String sourceBranch,
            String targetBranch,
            String prUrl,
            String status,
            String reviewStatus,
            boolean isDraft,
            Instant mergedAt,
            UUID repositoryId,
            String repositoryName,
            String linkType,
            Instant linkedAt,
            String linkedByName
    ) {}

    public record IssueDevelopmentResponse(
            UUID issueId,
            List<LinkedBranchResponse> branches,
            List<LinkedCommitResponse> commits,
            List<LinkedPullRequestResponse> pullRequests,
            int totalLinks
    ) {}

    public record ManualLinkRequest(
            String artifactType, // "BRANCH", "COMMIT", "PULL_REQUEST"
            UUID artifactId,     // Optional: direct ID if known
            String identifier,   // Optional: commit SHA, PR number, or branch name
            UUID repositoryId    // Optional: repository scope for identifier resolution
    ) {}

    public record DevelopmentCandidateResponse(
            UUID id,
            String artifactType, // "BRANCH", "COMMIT", "PULL_REQUEST"
            String identifier,
            String title,
            String subtitle,
            String url,
            Instant timestamp,
            UUID repositoryId,
            String repositoryName
    ) {}

    public record DevelopmentSummary(
            int totalRepositories,
            int totalBranches,
            int totalCommits,
            int totalPullRequests,
            int openPullRequests,
            int mergedPullRequests
    ) {}

    public record ProjectDevelopmentOverviewResponse(
            List<ConnectedRepositoryResponse> connectedRepositories,
            List<LinkedCommitResponse> recentCommits,
            List<LinkedPullRequestResponse> recentPullRequests,
            List<LinkedBranchResponse> recentBranches,
            DevelopmentSummary summary
    ) {}
}
