package com.nextrack.workspace.api.mapper;

import com.nextrack.workspace.api.dto.response.WorkspaceMemberResponse;
import com.nextrack.workspace.api.dto.response.WorkspaceResponse;
import com.nextrack.workspace.domain.entity.Workspace;
import com.nextrack.workspace.domain.entity.WorkspaceMembership;

public final class WorkspaceMapper {

    private WorkspaceMapper() {}

    public static WorkspaceResponse toResponse(Workspace workspace) {
        if (workspace == null) {
            return null;
        }
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getSlug(),
                workspace.getOwnerUserId(),
                workspace.getCreatedAt()
        );
    }

    public static WorkspaceMemberResponse toMemberResponse(WorkspaceMembership membership) {
        if (membership == null) {
            return null;
        }
        return new WorkspaceMemberResponse(
                membership.getWorkspaceId(),
                membership.getUserId(),
                membership.getRole().name(),
                membership.getJoinedAt()
        );
    }
}
