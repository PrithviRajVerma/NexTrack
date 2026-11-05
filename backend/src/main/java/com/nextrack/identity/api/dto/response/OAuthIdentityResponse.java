package com.nextrack.identity.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record OAuthIdentityResponse(
        UUID id,
        String provider,
        String providerUserId,
        String providerEmail,
        Instant createdAt
) {}
