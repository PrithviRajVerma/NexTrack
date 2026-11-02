package com.nextrack.project.api.dto.request;

import com.nextrack.project.domain.entity.ProjectRole;
import java.util.UUID;

public record AddProjectMemberRequest(UUID userId, ProjectRole role) {}
