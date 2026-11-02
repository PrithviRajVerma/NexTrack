package com.nextrack.project.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects", uniqueConstraints = {
        @UniqueConstraint(name = "uq_projects_workspace_project_key", columnNames = {"workspace_id", "project_key"})
})
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(nullable = false)
    private String name;

    @Column(name = "project_key", nullable = false, length = 10)
    private String projectKey;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "archived_at")
    private Instant archivedAt;

    protected Project() {}

    public Project(UUID workspaceId, String name, String projectKey, String description, UUID createdBy) {
        this.workspaceId = workspaceId;
        this.name = name.trim();
        this.projectKey = projectKey.toUpperCase().trim();
        this.description = description;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public String getName() { return name; }
    public String getProjectKey() { return projectKey; }
    public String getDescription() { return description; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getArchivedAt() { return archivedAt; }

    public void update(String name, String description) {
        this.name = name.trim();
        this.description = description;
    }

    public void archive() {
        this.archivedAt = Instant.now();
    }
}
