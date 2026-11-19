package com.nextrack.development.domain.repository;

import com.nextrack.development.domain.entity.IssueDevelopmentBranch;
import com.nextrack.development.domain.entity.IssueDevelopmentBranchId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IssueDevelopmentBranchRepository extends JpaRepository<IssueDevelopmentBranch, IssueDevelopmentBranchId> {
    List<IssueDevelopmentBranch> findAllByIssueId(UUID issueId);
    boolean existsByIssueIdAndBranchId(UUID issueId, UUID branchId);
    void deleteByIssueIdAndBranchId(UUID issueId, UUID branchId);
}
