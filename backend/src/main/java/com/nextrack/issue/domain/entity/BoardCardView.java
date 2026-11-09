package com.nextrack.issue.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "v_board_cards")
public class BoardCardView {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "project_key", nullable = false)
    private String projectKey;

    @Column(name = "issue_number", nullable = false)
    private long issueNumber;

    @Column(name = "formatted_key", nullable = false)
    private String formattedKey;

    @Column(name = "title", nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private IssueStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private IssuePriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false)
    private IssueType issueType;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "assignee_name")
    private String assigneeName;

    @Column(name = "assignee_email")
    private String assigneeEmail;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "story_points")
    private Integer storyPoints;

    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "comment_count", nullable = false)
    private long commentCount;

    @Column(name = "attachment_count", nullable = false)
    private long attachmentCount;

    @Column(name = "pr_count", nullable = false)
    private long prCount;

    @Column(name = "commit_count", nullable = false)
    private long commitCount;

    @Column(name = "branch_count", nullable = false)
    private long branchCount;

    protected BoardCardView() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public long getIssueNumber() {
        return issueNumber;
    }

    public String getFormattedKey() {
        return formattedKey;
    }

    public String getTitle() {
        return title;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public IssuePriority getPriority() {
        return priority;
    }

    public IssueType getIssueType() {
        return issueType;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public String getAssigneeEmail() {
        return assigneeEmail;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    public Integer getStoryPoints() {
        return storyPoints;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getCommentCount() {
        return commentCount;
    }

    public long getAttachmentCount() {
        return attachmentCount;
    }

    public long getPrCount() {
        return prCount;
    }

    public long getCommitCount() {
        return commitCount;
    }

    public long getBranchCount() {
        return branchCount;
    }
}
