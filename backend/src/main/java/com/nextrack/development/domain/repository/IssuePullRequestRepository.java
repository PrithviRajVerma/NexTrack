package com.nextrack.development.domain.repository;

import com.nextrack.development.domain.entity.IssuePullRequest;
import com.nextrack.development.domain.entity.IssuePullRequestId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface IssuePullRequestRepository extends JpaRepository<IssuePullRequest, IssuePullRequestId> {

    @Query("SELECT p FROM IssuePullRequest p WHERE p.id.issueId = :issueId")
    List<IssuePullRequest> findAllByIdIssueId(@Param("issueId") UUID issueId);

    @Query("SELECT COUNT(p) > 0 FROM IssuePullRequest p WHERE p.id.issueId = :issueId AND p.id.pullRequestId = :pullRequestId")
    boolean existsByIdIssueIdAndIdPullRequestId(@Param("issueId") UUID issueId, @Param("pullRequestId") UUID pullRequestId);

    @Modifying
    @Query("DELETE FROM IssuePullRequest p WHERE p.id.issueId = :issueId AND p.id.pullRequestId = :pullRequestId")
    void deleteByIdIssueIdAndIdPullRequestId(@Param("issueId") UUID issueId, @Param("pullRequestId") UUID pullRequestId);
}
