package com.nextrack.workspace.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceMemberResponse(
        UUID workspaceId,
        UUID userId,
        String role,
        Instant joinedAt
) {}
