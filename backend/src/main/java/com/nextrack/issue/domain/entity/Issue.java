package com.nextrack.issue.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "issues")
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "issue_number", nullable = false)
    private long issueNumber;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false)
    private IssueType issueType = IssueType.TASK;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueStatus status = IssueStatus.BACKLOG;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssuePriority priority = IssuePriority.MEDIUM;

    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "story_points")
    private Integer storyPoints;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "archived_at")
    private Instant archivedAt;

    protected Issue() {}

    public Issue(
            UUID projectId,
            long issueNumber,
            String title,
            String description,
            IssueType issueType,
            IssuePriority priority,
            UUID reporterId,
            UUID assigneeId,
            Instant dueDate,
            Integer storyPoints
    ) {
        this.projectId = projectId;
        this.issueNumber = issueNumber;
        this.title = title.trim();
        this.description = description != null ? description.trim() : null;
        this.issueType = issueType != null ? issueType : IssueType.TASK;
        this.status = IssueStatus.BACKLOG;
        this.priority = priority != null ? priority : IssuePriority.MEDIUM;
        this.reporterId = reporterId;
        this.assigneeId = assigneeId;
        this.dueDate = dueDate;
        this.storyPoints = storyPoints;
        this.version = 0L;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public long getIssueNumber() { return issueNumber; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public IssueType getIssueType() { return issueType; }
    public IssueStatus getStatus() { return status; }
    public IssuePriority getPriority() { return priority; }
    public UUID getReporterId() { return reporterId; }
    public UUID getAssigneeId() { return assigneeId; }
    public Instant getDueDate() { return dueDate; }
    public Integer getStoryPoints() { return storyPoints; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getArchivedAt() { return archivedAt; }

    public void updateDetails(
            String title,
            String description,
            IssueType issueType,
            IssuePriority priority,
            UUID assigneeId,
            Instant dueDate,
            Integer storyPoints
    ) {
        if (title != null && !title.isBlank()) {
            this.title = title.trim();
        }
        if (description != null) {
            this.description = description.trim();
        }
        if (issueType != null) {
            this.issueType = issueType;
        }
        if (priority != null) {
            this.priority = priority;
        }
        this.assigneeId = assigneeId;
        this.dueDate = dueDate;
        this.storyPoints = storyPoints;
        this.updatedAt = Instant.now();
    }

    public void updateStatus(IssueStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void archive() {
        this.archivedAt = Instant.now();
        this.status = IssueStatus.ARCHIVED;
        this.updatedAt = Instant.now();
    }
}
