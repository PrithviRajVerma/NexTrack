package com.nextrack.development.domain.repository;

import com.nextrack.development.domain.entity.IssueDevelopmentCommit;
import com.nextrack.development.domain.entity.IssueDevelopmentCommitId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IssueDevelopmentCommitRepository extends JpaRepository<IssueDevelopmentCommit, IssueDevelopmentCommitId> {
    List<IssueDevelopmentCommit> findAllByIssueId(UUID issueId);
    boolean existsByIssueIdAndCommitId(UUID issueId, UUID commitId);
    void deleteByIssueIdAndCommitId(UUID issueId, UUID commitId);
}
