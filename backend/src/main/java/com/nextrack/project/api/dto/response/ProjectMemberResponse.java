package com.nextrack.project.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProjectMemberResponse(
        UUID projectId,
        UUID userId,
        String role,
        Instant joinedAt
) {}
