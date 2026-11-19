package com.nextrack.development.domain.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class IssueDevelopmentCommitId implements Serializable {

    private UUID issueId;
    private UUID commitId;

    public IssueDevelopmentCommitId() {}

    public IssueDevelopmentCommitId(UUID issueId, UUID commitId) {
        this.issueId = issueId;
        this.commitId = commitId;
    }

    public UUID getIssueId() { return issueId; }
    public UUID getCommitId() { return commitId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IssueDevelopmentCommitId that)) return false;
        return Objects.equals(issueId, that.issueId) && Objects.equals(commitId, that.commitId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(issueId, commitId);
    }
}
