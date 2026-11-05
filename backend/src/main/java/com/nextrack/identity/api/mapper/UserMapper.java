package com.nextrack.identity.api.mapper;

import com.nextrack.identity.api.dto.response.UserDto;
import com.nextrack.identity.domain.entity.User;

public final class UserMapper {

    private UserMapper() {}

    public static UserDto toDto(User user) {
        if (user == null) {
            return null;
        }
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.hasPassword(),
                user.getStatus().name(),
                user.getCreatedAt()
        );
    }
}
