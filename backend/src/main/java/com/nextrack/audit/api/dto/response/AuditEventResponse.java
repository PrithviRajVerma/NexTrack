package com.nextrack.audit.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        UUID actorId,
        String entityType,
        UUID entityId,
        String action,
        String beforePayload,
        String afterPayload,
        String traceId,
        Instant createdAt
) {}
