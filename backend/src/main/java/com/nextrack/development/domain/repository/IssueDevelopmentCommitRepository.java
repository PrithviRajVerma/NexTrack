package com.nextrack.development.domain.repository;

import com.nextrack.development.domain.entity.IssueDevelopmentCommit;
import com.nextrack.development.domain.entity.IssueDevelopmentCommitId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface IssueDevelopmentCommitRepository extends JpaRepository<IssueDevelopmentCommit, IssueDevelopmentCommitId> {

    @Query("SELECT c FROM IssueDevelopmentCommit c WHERE c.id.issueId = :issueId")
    List<IssueDevelopmentCommit> findAllByIdIssueId(@Param("issueId") UUID issueId);

    @Query("SELECT COUNT(c) > 0 FROM IssueDevelopmentCommit c WHERE c.id.issueId = :issueId AND c.id.commitId = :commitId")
    boolean existsByIdIssueIdAndIdCommitId(@Param("issueId") UUID issueId, @Param("commitId") UUID commitId);

    @Modifying
    @Query("DELETE FROM IssueDevelopmentCommit c WHERE c.id.issueId = :issueId AND c.id.commitId = :commitId")
    void deleteByIdIssueIdAndIdCommitId(@Param("issueId") UUID issueId, @Param("commitId") UUID commitId);
}
