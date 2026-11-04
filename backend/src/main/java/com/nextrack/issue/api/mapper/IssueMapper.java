package com.nextrack.issue.api.mapper;

import com.nextrack.issue.api.dto.response.IssueResponse;
import com.nextrack.issue.domain.entity.Issue;

public final class IssueMapper {

    private IssueMapper() {}

    public static IssueResponse toResponse(Issue issue, String projectKey) {
        String issueKey = projectKey + "-" + issue.getIssueNumber();
        return new IssueResponse(
                issue.getId(),
                issue.getProjectId(),
                projectKey,
                issue.getIssueNumber(),
                issueKey,
                issue.getTitle(),
                issue.getDescription(),
                issue.getIssueType(),
                issue.getStatus(),
                issue.getPriority(),
                issue.getReporterId(),
                issue.getAssigneeId(),
                issue.getDueDate(),
                issue.getStoryPoints(),
                issue.getVersion(),
                issue.getCreatedAt(),
                issue.getUpdatedAt()
        );
    }
}
