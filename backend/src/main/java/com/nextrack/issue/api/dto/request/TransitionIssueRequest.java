package com.nextrack.issue.api.dto.request;

import com.nextrack.issue.domain.entity.IssueStatus;
import jakarta.validation.constraints.NotNull;

public record TransitionIssueRequest(
        @NotNull(message = "Target status is required")
        IssueStatus targetStatus,

        @NotNull(message = "Version is required for optimistic locking")
        Long version
) {}
