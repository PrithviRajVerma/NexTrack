package com.nextrack.development.domain.repository;

import com.nextrack.development.domain.entity.PullRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PullRequestRepository extends JpaRepository<PullRequest, UUID> {
    Optional<PullRequest> findByRepositoryIdAndPrNumber(UUID repositoryId, Integer prNumber);
    Optional<PullRequest> findByRepositoryIdAndProviderPrId(UUID repositoryId, Long providerPrId);
    List<PullRequest> findAllByRepositoryIdOrderByCreatedAtDesc(UUID repositoryId);
    List<PullRequest> findAllByRepositoryIdInOrderByCreatedAtDesc(Collection<UUID> repositoryIds, Pageable pageable);
    List<PullRequest> findAllByRepositoryIdInAndTitleContainingIgnoreCase(
            Collection<UUID> repositoryIds, String titleQuery, Pageable pageable
    );
    long countByRepositoryIdInAndStatus(Collection<UUID> repositoryIds, String status);
}
