package com.nextrack.issue.domain.repository;

import com.nextrack.issue.domain.entity.Issue;
import com.nextrack.issue.domain.entity.IssueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IssueRepository extends JpaRepository<Issue, UUID> {
    Optional<Issue> findByProjectIdAndIssueNumber(UUID projectId, long issueNumber);
    Page<Issue> findByProjectId(UUID projectId, Pageable pageable);
    Page<Issue> findByProjectIdAndStatus(UUID projectId, IssueStatus status, Pageable pageable);
    boolean existsByProjectIdAndIssueNumber(UUID projectId, long issueNumber);
}
