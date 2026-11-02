package com.nextrack.workspace.api;

import com.nextrack.workspace.domain.Workspace;
import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name,
        String slug,
        UUID ownerUserId,
        Instant createdAt
) {
    public static WorkspaceResponse from(Workspace workspace) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getSlug(),
                workspace.getOwnerUserId(),
                workspace.getCreatedAt()
        );
    }
}
