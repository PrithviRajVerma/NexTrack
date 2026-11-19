package com.nextrack.development.domain.repository;

import com.nextrack.development.domain.entity.DevelopmentCommit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DevelopmentCommitRepository extends JpaRepository<DevelopmentCommit, UUID> {
    Optional<DevelopmentCommit> findByRepositoryIdAndCommitSha(UUID repositoryId, String commitSha);
    Optional<DevelopmentCommit> findFirstByCommitSha(String commitSha);
    List<DevelopmentCommit> findAllByRepositoryIdOrderByCommittedAtDesc(UUID repositoryId);
    List<DevelopmentCommit> findAllByRepositoryIdInOrderByCommittedAtDesc(Collection<UUID> repositoryIds, Pageable pageable);
    List<DevelopmentCommit> findAllByRepositoryIdInAndCommitShaContainingIgnoreCaseOrMessageContainingIgnoreCase(
            Collection<UUID> repositoryIds, String shaQuery, String messageQuery, Pageable pageable
    );
}
