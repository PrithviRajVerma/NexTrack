package com.nextrack.development.domain.repository;

import com.nextrack.development.domain.entity.ConnectedRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConnectedRepositoryRepository extends JpaRepository<ConnectedRepository, UUID> {

    Optional<ConnectedRepository> findByInstallationIdAndExternalRepoId(UUID installationId, String externalRepoId);

    Optional<ConnectedRepository> findFirstByExternalRepoId(String externalRepoId);

    List<ConnectedRepository> findAllByInstallationId(UUID installationId);

    Optional<ConnectedRepository> findByFullName(String fullName);

    Optional<ConnectedRepository> findFirstByFullName(String fullName);
}
