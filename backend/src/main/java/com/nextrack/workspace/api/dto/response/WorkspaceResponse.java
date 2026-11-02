package com.nextrack.workspace.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name,
        String slug,
        UUID ownerUserId,
        Instant createdAt
) {}
