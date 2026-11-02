package com.nextrack.project.api.mapper;

import com.nextrack.project.api.dto.response.ProjectMemberResponse;
import com.nextrack.project.api.dto.response.ProjectResponse;
import com.nextrack.project.domain.entity.Project;
import com.nextrack.project.domain.entity.ProjectMembership;

public final class ProjectMapper {

    private ProjectMapper() {}

    public static ProjectResponse toResponse(Project project) {
        if (project == null) {
            return null;
        }
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

    public static ProjectMemberResponse toMemberResponse(ProjectMembership membership) {
        if (membership == null) {
            return null;
        }
        return new ProjectMemberResponse(
                membership.getProjectId(),
                membership.getUserId(),
                membership.getRole().name(),
                membership.getJoinedAt()
        );
    }
}
