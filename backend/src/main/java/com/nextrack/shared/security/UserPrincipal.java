package com.nextrack.shared.security;

import java.security.Principal;
import java.util.UUID;

public record UserPrincipal(
        UUID id,
        String email,
        String displayName
) implements Principal {
    @Override
    public String getName() {
        return id.toString();
    }
}
