package com.nextrack.identity.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String displayName,
        String status,
        Instant createdAt
) {}
