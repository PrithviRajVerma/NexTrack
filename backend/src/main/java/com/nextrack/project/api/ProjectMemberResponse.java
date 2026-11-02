package com.nextrack.project.api;

import com.nextrack.project.domain.ProjectMembership;
import java.time.Instant;
import java.util.UUID;

public record ProjectMemberResponse(
        UUID projectId,
        UUID userId,
        String role,
        Instant joinedAt
) {
    public static ProjectMemberResponse from(ProjectMembership membership) {
        return new ProjectMemberResponse(
                membership.getProjectId(),
                membership.getUserId(),
                membership.getRole().name(),
                membership.getJoinedAt()
        );
    }
}
