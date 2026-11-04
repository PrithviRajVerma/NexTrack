package com.nextrack.issue.api.dto.request;

import com.nextrack.issue.domain.entity.IssuePriority;
import com.nextrack.issue.domain.entity.IssueType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateIssueRequest(
        @NotBlank(message = "Title must not be blank")
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
