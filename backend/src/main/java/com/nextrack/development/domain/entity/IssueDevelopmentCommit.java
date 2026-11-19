package com.nextrack.development.domain.entity;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "issue_development_commits")
@IdClass(IssueDevelopmentCommitId.class)
public class IssueDevelopmentCommit implements Persistable<IssueDevelopmentCommitId> {

    @Id
    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Id
    @Column(name = "commit_id", nullable = false)
    private UUID commitId;

    @Column(name = "link_type", nullable = false, length = 32)
    private String linkType = "AUTO_EXTRACTED";

    @Column(name = "linked_by")
    private UUID linkedBy;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt = Instant.now();

    @Transient
    private boolean isNew = true;

    protected IssueDevelopmentCommit() {
        this.isNew = false;
    }

    public IssueDevelopmentCommit(UUID issueId, UUID commitId, String linkType, UUID linkedBy) {
        this.issueId = issueId;
        this.commitId = commitId;
        this.linkType = linkType != null ? linkType : "AUTO_EXTRACTED";
        this.linkedBy = linkedBy;
        this.linkedAt = Instant.now();
        this.isNew = true;
    }

    @Override
    public IssueDevelopmentCommitId getId() {
        return new IssueDevelopmentCommitId(issueId, commitId);
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    public UUID getIssueId() { return issueId; }
    public UUID getCommitId() { return commitId; }
    public String getLinkType() { return linkType; }
    public UUID getLinkedBy() { return linkedBy; }
    public Instant getLinkedAt() { return linkedAt; }
}
