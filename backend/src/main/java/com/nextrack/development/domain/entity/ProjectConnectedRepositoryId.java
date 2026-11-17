package com.nextrack.development.domain.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ProjectConnectedRepositoryId implements Serializable {

    private UUID projectId;
    private UUID repositoryId;

    public ProjectConnectedRepositoryId() {}

    public ProjectConnectedRepositoryId(UUID projectId, UUID repositoryId) {
        this.projectId = projectId;
        this.repositoryId = repositoryId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getRepositoryId() {
        return repositoryId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectConnectedRepositoryId that)) return false;
        return Objects.equals(projectId, that.projectId) && Objects.equals(repositoryId, that.repositoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, repositoryId);
    }
}
