package com.nextrack.development.domain.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class IssueDevelopmentBranchId implements Serializable {

    private UUID issueId;
    private UUID branchId;

    public IssueDevelopmentBranchId() {}

    public IssueDevelopmentBranchId(UUID issueId, UUID branchId) {
        this.issueId = issueId;
        this.branchId = branchId;
    }

    public UUID getIssueId() { return issueId; }
    public UUID getBranchId() { return branchId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IssueDevelopmentBranchId that)) return false;
        return Objects.equals(issueId, that.issueId) && Objects.equals(branchId, that.branchId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(issueId, branchId);
    }
}
