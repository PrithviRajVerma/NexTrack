package com.nextrack.identity.api;

import com.nextrack.identity.domain.User;
import java.time.Instant;
import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String displayName,
        String status,
        Instant createdAt
) {
    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getStatus().name(),
                user.getCreatedAt()
        );
    }
}
