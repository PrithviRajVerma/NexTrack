package com.nextrack.development.domain.repository;

import com.nextrack.development.domain.entity.IntegrationInstallation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntegrationInstallationRepository extends JpaRepository<IntegrationInstallation, UUID> {

    Optional<IntegrationInstallation> findByProviderAndInstallationId(String provider, Long installationId);

    List<IntegrationInstallation> findAllByWorkspaceId(UUID workspaceId);

    boolean existsByProviderAndInstallationId(String provider, Long installationId);
}
