package com.nextrack.development.domain.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class IssuePullRequestId implements Serializable {

    private UUID issueId;
    private UUID pullRequestId;

    public IssuePullRequestId() {}

    public IssuePullRequestId(UUID issueId, UUID pullRequestId) {
        this.issueId = issueId;
        this.pullRequestId = pullRequestId;
    }

    public UUID getIssueId() { return issueId; }
    public UUID getPullRequestId() { return pullRequestId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IssuePullRequestId that)) return false;
        return Objects.equals(issueId, that.issueId) && Objects.equals(pullRequestId, that.pullRequestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(issueId, pullRequestId);
    }
}
