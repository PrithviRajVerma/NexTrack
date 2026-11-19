package com.nextrack.development.domain.repository;

import com.nextrack.development.domain.entity.DevelopmentBranch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DevelopmentBranchRepository extends JpaRepository<DevelopmentBranch, UUID> {
    Optional<DevelopmentBranch> findByRepositoryIdAndName(UUID repositoryId, String name);
    List<DevelopmentBranch> findAllByRepositoryId(UUID repositoryId);
    List<DevelopmentBranch> findAllByRepositoryIdIn(Collection<UUID> repositoryIds);
}
