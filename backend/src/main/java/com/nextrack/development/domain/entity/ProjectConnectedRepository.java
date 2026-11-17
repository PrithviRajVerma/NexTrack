package com.nextrack.development.domain.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "project_connected_repositories")
@IdClass(ProjectConnectedRepositoryId.class)
public class ProjectConnectedRepository {

    @Id
    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Id
    @Column(name = "repository_id", nullable = false)
    private UUID repositoryId;

    @Column(name = "connected_by", nullable = false)
    private UUID connectedBy;

    @Column(name = "connected_at", nullable = false, updatable = false)
    private Instant connectedAt = Instant.now();

    protected ProjectConnectedRepository() {}

    public ProjectConnectedRepository(UUID projectId, UUID repositoryId, UUID connectedBy) {
        this.projectId = projectId;
        this.repositoryId = repositoryId;
        this.connectedBy = connectedBy;
        this.connectedAt = Instant.now();
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getRepositoryId() {
        return repositoryId;
    }

    public UUID getConnectedBy() {
        return connectedBy;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }
}
