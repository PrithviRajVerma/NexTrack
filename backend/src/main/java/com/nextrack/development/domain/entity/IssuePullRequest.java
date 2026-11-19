package com.nextrack.development.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "issue_pull_requests")
@IdClass(IssuePullRequestId.class)
public class IssuePullRequest {

    @Id
    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Id
    @Column(name = "pull_request_id", nullable = false)
    private UUID pullRequestId;

    @Column(name = "link_type", nullable = false, length = 32)
    private String linkType = "AUTO_EXTRACTED";

    @Column(name = "linked_by")
    private UUID linkedBy;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt = Instant.now();

    protected IssuePullRequest() {}

    public IssuePullRequest(UUID issueId, UUID pullRequestId, String linkType, UUID linkedBy) {
        this.issueId = issueId;
        this.pullRequestId = pullRequestId;
        this.linkType = linkType != null ? linkType : "AUTO_EXTRACTED";
        this.linkedBy = linkedBy;
        this.linkedAt = Instant.now();
    }

    public UUID getIssueId() { return issueId; }
    public UUID getPullRequestId() { return pullRequestId; }
    public String getLinkType() { return linkType; }
    public UUID getLinkedBy() { return linkedBy; }
    public Instant getLinkedAt() { return linkedAt; }
}
