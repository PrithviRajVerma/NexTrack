package com.nextrack.project.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID workspaceId,
        String name,
        String projectKey,
        String description,
        UUID createdBy,
        Instant createdAt
) {}
