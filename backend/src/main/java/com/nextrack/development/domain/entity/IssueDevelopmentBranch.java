package com.nextrack.development.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "issue_development_branches")
@IdClass(IssueDevelopmentBranchId.class)
public class IssueDevelopmentBranch {

    @Id
    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Id
    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "link_type", nullable = false, length = 32)
    private String linkType = "AUTO_EXTRACTED";

    @Column(name = "linked_by")
    private UUID linkedBy;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt = Instant.now();

    protected IssueDevelopmentBranch() {}

    public IssueDevelopmentBranch(UUID issueId, UUID branchId, String linkType, UUID linkedBy) {
        this.issueId = issueId;
        this.branchId = branchId;
        this.linkType = linkType != null ? linkType : "AUTO_EXTRACTED";
        this.linkedBy = linkedBy;
        this.linkedAt = Instant.now();
    }

    public UUID getIssueId() { return issueId; }
    public UUID getBranchId() { return branchId; }
    public String getLinkType() { return linkType; }
    public UUID getLinkedBy() { return linkedBy; }
    public Instant getLinkedAt() { return linkedAt; }
}
