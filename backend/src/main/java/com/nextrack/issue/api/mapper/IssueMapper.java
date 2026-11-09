package com.nextrack.issue.api.mapper;

import com.nextrack.issue.api.dto.response.BoardCardResponse;
import com.nextrack.issue.api.dto.response.IssueResponse;
import com.nextrack.issue.domain.entity.BoardCardView;
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

    public static BoardCardResponse toBoardCardResponse(BoardCardView view) {
        return new BoardCardResponse(
                view.getId(),
                view.getProjectId(),
                view.getProjectKey(),
                view.getIssueNumber(),
                view.getFormattedKey(),
                view.getTitle(),
                view.getStatus(),
                view.getPriority(),
                view.getIssueType(),
                view.getAssigneeId(),
                view.getAssigneeName(),
                view.getAssigneeEmail(),
                view.getDueDate(),
                view.getStoryPoints(),
                view.getVersion(),
                view.getCreatedAt(),
                view.getUpdatedAt(),
                view.getCommentCount(),
                view.getAttachmentCount(),
                view.getPrCount(),
                view.getCommitCount(),
                view.getBranchCount()
        );
    }
}

