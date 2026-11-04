package com.nextrack.issue.api.dto.request;

import com.nextrack.issue.domain.entity.IssuePriority;
import com.nextrack.issue.domain.entity.IssueType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record UpdateIssueRequest(
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        String description,
        IssueType issueType,
        IssuePriority priority,
        UUID assigneeId,
        Instant dueDate,

        @Min(value = 0, message = "Story points must be non-negative")
        @Max(value = 100, message = "Story points must not exceed 100")
        Integer storyPoints
) {}
