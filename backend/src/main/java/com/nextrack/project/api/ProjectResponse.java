package com.nextrack.project.api;

import com.nextrack.project.domain.Project;
import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID workspaceId,
        String name,
        String projectKey,
        String description,
        UUID createdBy,
        Instant createdAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getWorkspaceId(),
                project.getName(),
                project.getProjectKey(),
                project.getDescription(),
                project.getCreatedBy(),
                project.getCreatedAt()
        );
    }
}
