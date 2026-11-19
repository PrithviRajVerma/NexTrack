package com.nextrack.development.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pull_requests", uniqueConstraints = {
        @UniqueConstraint(name = "uq_pr_repo_num", columnNames = {"repository_id", "pr_number"}),
        @UniqueConstraint(name = "uq_pr_repo_provider_id", columnNames = {"repository_id", "provider_pr_id"})
})
public class PullRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "repository_id", nullable = false)
    private UUID repositoryId;

    @Column(name = "provider_pr_id", nullable = false)
    private Long providerPrId;

    @Column(name = "pr_number", nullable = false)
    private Integer prNumber;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_branch", nullable = false)
    private String sourceBranch;

    @Column(name = "target_branch", nullable = false)
    private String targetBranch;

    @Column(name = "pr_url", nullable = false, length = 512)
    private String prUrl;

    @Column(nullable = false, length = 32)
    private String status = "OPEN"; // OPEN, CLOSED, MERGED

    @Column(name = "review_status", length = 32)
    private String reviewStatus; // PENDING, APPROVED, CHANGES_REQUESTED

    @Column(name = "is_draft", nullable = false)
    private boolean isDraft = false;

    @Column(name = "merged_at")
    private Instant mergedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected PullRequest() {}

    public PullRequest(
            UUID repositoryId,
            Long providerPrId,
            Integer prNumber,
            String title,
            String description,
            String sourceBranch,
            String targetBranch,
            String prUrl,
            String status,
            String reviewStatus,
            boolean isDraft,
            Instant mergedAt
    ) {
        this.repositoryId = repositoryId;
        this.providerPrId = providerPrId;
        this.prNumber = prNumber;
        this.title = title;
        this.description = description;
        this.sourceBranch = sourceBranch;
        this.targetBranch = targetBranch;
        this.prUrl = prUrl;
        this.status = status != null ? status : "OPEN";
        this.reviewStatus = reviewStatus;
        this.isDraft = isDraft;
        this.mergedAt = mergedAt;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getRepositoryId() { return repositoryId; }
    public Long getProviderPrId() { return providerPrId; }
    public Integer getPrNumber() { return prNumber; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getSourceBranch() { return sourceBranch; }
    public String getTargetBranch() { return targetBranch; }
    public String getPrUrl() { return prUrl; }
    public String getStatus() { return status; }
    public String getReviewStatus() { return reviewStatus; }
    public boolean isDraft() { return isDraft; }
    public Instant getMergedAt() { return mergedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void updateDetails(
            String title,
            String description,
            String sourceBranch,
            String targetBranch,
            String status,
            String reviewStatus,
            boolean isDraft,
            Instant mergedAt
    ) {
        this.title = title;
        this.description = description;
        this.sourceBranch = sourceBranch;
        this.targetBranch = targetBranch;
        this.status = status;
        this.reviewStatus = reviewStatus;
        this.isDraft = isDraft;
        this.mergedAt = mergedAt;
        this.updatedAt = Instant.now();
    }
}
