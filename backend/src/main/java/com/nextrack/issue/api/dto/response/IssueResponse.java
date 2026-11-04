package com.nextrack.issue.api.dto.response;

import com.nextrack.issue.domain.entity.IssuePriority;
import com.nextrack.issue.domain.entity.IssueStatus;
import com.nextrack.issue.domain.entity.IssueType;

import java.time.Instant;
import java.util.UUID;

public record IssueResponse(
        UUID id,
        UUID projectId,
        String projectKey,
        long issueNumber,
        String issueKey,
        String title,
        String description,
        IssueType issueType,
        IssueStatus status,
        IssuePriority priority,
        UUID reporterId,
        UUID assigneeId,
        Instant dueDate,
        Integer storyPoints,
        long version,
        Instant createdAt,
        Instant updatedAt
) {}
