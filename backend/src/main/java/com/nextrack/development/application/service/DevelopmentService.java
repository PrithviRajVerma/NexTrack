package com.nextrack.development.application.service;

import com.nextrack.development.api.dto.DevelopmentDtos.*;
import com.nextrack.development.domain.entity.ConnectedRepository;
import com.nextrack.development.domain.entity.IntegrationInstallation;
import com.nextrack.development.domain.entity.ProjectConnectedRepository;
import com.nextrack.development.domain.repository.ConnectedRepositoryRepository;
import com.nextrack.development.domain.repository.IntegrationInstallationRepository;
import com.nextrack.development.domain.repository.ProjectConnectedRepositoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DevelopmentService {

    private final IntegrationInstallationRepository installationRepository;
    private final ConnectedRepositoryRepository connectedRepositoryRepository;
    private final ProjectConnectedRepositoryRepository projectConnectedRepositoryRepository;

    public DevelopmentService(
            IntegrationInstallationRepository installationRepository,
            ConnectedRepositoryRepository connectedRepositoryRepository,
            ProjectConnectedRepositoryRepository projectConnectedRepositoryRepository
    ) {
        this.installationRepository = installationRepository;
        this.connectedRepositoryRepository = connectedRepositoryRepository;
        this.projectConnectedRepositoryRepository = projectConnectedRepositoryRepository;
    }

    @Transactional
    public InstallationResponse registerInstallation(UUID workspaceId, CreateInstallationRequest request) {
        IntegrationInstallation installation = new IntegrationInstallation(
                workspaceId,
                "GITHUB",
                request.installationId(),
                request.accountName(),
                request.repositorySelection(),
                request.permissions(),
                null
        );

        IntegrationInstallation saved = installationRepository.save(installation);
        return toInstallationResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<InstallationResponse> listInstallations(UUID workspaceId) {
        return installationRepository.findAllByWorkspaceId(workspaceId)
                .stream()
                .map(this::toInstallationResponse)
                .toList();
    }

    @Transactional
    public ConnectedRepositoryResponse connectRepository(UUID installationId, ConnectRepositoryRequest request) {
        ConnectedRepository repo = new ConnectedRepository(
                installationId,
                "GITHUB",
                request.externalRepoId(),
                request.name(),
                request.fullName(),
                request.htmlUrl(),
                request.defaultBranch(),
                true
        );

        ConnectedRepository saved = connectedRepositoryRepository.save(repo);
        return toConnectedRepositoryResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ConnectedRepositoryResponse> listRepositoriesForInstallation(UUID installationId) {
        return connectedRepositoryRepository.findAllByInstallationId(installationId)
                .stream()
                .map(this::toConnectedRepositoryResponse)
                .toList();
    }

    @Transactional
    public void linkRepositoryToProject(UUID projectId, UUID repositoryId, UUID connectedBy) {
        if (!projectConnectedRepositoryRepository.existsByProjectIdAndRepositoryId(projectId, repositoryId)) {
            ProjectConnectedRepository link = new ProjectConnectedRepository(projectId, repositoryId, connectedBy);
            projectConnectedRepositoryRepository.save(link);
        }
    }

    @Transactional
    public void unlinkRepositoryFromProject(UUID projectId, UUID repositoryId) {
        projectConnectedRepositoryRepository.deleteByProjectIdAndRepositoryId(projectId, repositoryId);
    }

    @Transactional(readOnly = true)
    public List<ConnectedRepositoryResponse> listProjectRepositories(UUID projectId) {
        List<UUID> repoIds = projectConnectedRepositoryRepository.findAllByProjectId(projectId)
                .stream()
                .map(ProjectConnectedRepository::getRepositoryId)
                .toList();

        return connectedRepositoryRepository.findAllById(repoIds)
                .stream()
                .map(this::toConnectedRepositoryResponse)
                .toList();
    }

    private InstallationResponse toInstallationResponse(IntegrationInstallation inst) {
        return new InstallationResponse(
                inst.getId(),
                inst.getWorkspaceId(),
                inst.getProvider(),
                inst.getInstallationId(),
                inst.getAccountName(),
                inst.getRepositorySelection(),
                inst.getCreatedAt()
        );
    }

    private ConnectedRepositoryResponse toConnectedRepositoryResponse(ConnectedRepository repo) {
        return new ConnectedRepositoryResponse(
                repo.getId(),
                repo.getInstallationId(),
                repo.getProvider(),
                repo.getExternalRepoId(),
                repo.getName(),
                repo.getFullName(),
                repo.getHtmlUrl(),
                repo.getDefaultBranch(),
                repo.isActive(),
                repo.getLastSyncedAt()
        );
    }
}
