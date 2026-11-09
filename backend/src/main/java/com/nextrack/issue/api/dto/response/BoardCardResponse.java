package com.nextrack.issue.api.dto.response;

import com.nextrack.issue.domain.entity.IssuePriority;
import com.nextrack.issue.domain.entity.IssueStatus;
import com.nextrack.issue.domain.entity.IssueType;

import java.time.Instant;
import java.util.UUID;

public record BoardCardResponse(
        UUID id,
        UUID projectId,
        String projectKey,
        long issueNumber,
        String formattedKey,
        String title,
        IssueStatus status,
        IssuePriority priority,
        IssueType issueType,
        UUID assigneeId,
        String assigneeName,
        String assigneeEmail,
        Instant dueDate,
        Integer storyPoints,
        long version,
        Instant createdAt,
        Instant updatedAt,
        long commentCount,
        long attachmentCount,
        long prCount,
        long commitCount,
        long branchCount
) {}
