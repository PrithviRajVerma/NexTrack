package com.nextrack.development.api.dto;

import java.time.Instant;
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
}
