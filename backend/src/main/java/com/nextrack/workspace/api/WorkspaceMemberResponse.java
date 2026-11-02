package com.nextrack.workspace.api;

import com.nextrack.workspace.domain.WorkspaceMembership;
import java.time.Instant;
import java.util.UUID;

public record WorkspaceMemberResponse(
        UUID workspaceId,
        UUID userId,
        String role,
        Instant joinedAt
) {
    public static WorkspaceMemberResponse from(WorkspaceMembership membership) {
        return new WorkspaceMemberResponse(
                membership.getWorkspaceId(),
                membership.getUserId(),
                membership.getRole().name(),
                membership.getJoinedAt()
        );
    }
}
