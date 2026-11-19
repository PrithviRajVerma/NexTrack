package com.nextrack.development.domain.repository;

import com.nextrack.development.domain.entity.IssueDevelopmentBranch;
import com.nextrack.development.domain.entity.IssueDevelopmentBranchId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface IssueDevelopmentBranchRepository extends JpaRepository<IssueDevelopmentBranch, IssueDevelopmentBranchId> {

    @Query("SELECT b FROM IssueDevelopmentBranch b WHERE b.id.issueId = :issueId")
    List<IssueDevelopmentBranch> findAllByIdIssueId(@Param("issueId") UUID issueId);

    @Query("SELECT COUNT(b) > 0 FROM IssueDevelopmentBranch b WHERE b.id.issueId = :issueId AND b.id.branchId = :branchId")
    boolean existsByIdIssueIdAndIdBranchId(@Param("issueId") UUID issueId, @Param("branchId") UUID branchId);

    @Modifying
    @Query("DELETE FROM IssueDevelopmentBranch b WHERE b.id.issueId = :issueId AND b.id.branchId = :branchId")
    void deleteByIdIssueIdAndIdBranchId(@Param("issueId") UUID issueId, @Param("branchId") UUID branchId);
}
