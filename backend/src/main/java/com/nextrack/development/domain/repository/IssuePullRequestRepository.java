package com.nextrack.development.domain.repository;

import com.nextrack.development.domain.entity.IssuePullRequest;
import com.nextrack.development.domain.entity.IssuePullRequestId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IssuePullRequestRepository extends JpaRepository<IssuePullRequest, IssuePullRequestId> {
    List<IssuePullRequest> findAllByIssueId(UUID issueId);
    boolean existsByIssueIdAndPullRequestId(UUID issueId, UUID pullRequestId);
    void deleteByIssueIdAndPullRequestId(UUID issueId, UUID pullRequestId);
}
