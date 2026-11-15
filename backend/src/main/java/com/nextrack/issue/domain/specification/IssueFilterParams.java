package com.nextrack.issue.domain.specification;

import com.nextrack.issue.domain.entity.IssuePriority;
import com.nextrack.issue.domain.entity.IssueStatus;
import com.nextrack.issue.domain.entity.IssueType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IssueFilterParams(
        String search,
        List<IssueStatus> statuses,
        List<IssuePriority> priorities,
        List<IssueType> types,
        UUID assigneeId,
        UUID reporterId,
        Instant dueBefore,
        Instant dueAfter,
        Boolean isUnassigned,
        Boolean includeArchived
) {
    public static IssueFilterParams empty() {
        return new IssueFilterParams(null, null, null, null, null, null, null, null, null, false);
    }
}
