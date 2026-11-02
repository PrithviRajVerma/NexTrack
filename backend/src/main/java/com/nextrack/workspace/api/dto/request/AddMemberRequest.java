package com.nextrack.workspace.api.dto.request;

import com.nextrack.workspace.domain.entity.WorkspaceRole;
import java.util.UUID;

public record AddMemberRequest(UUID userId, WorkspaceRole role) {}
