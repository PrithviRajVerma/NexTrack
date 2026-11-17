package com.nextrack.development.domain.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "connected_repositories",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_connected_repo", columnNames = {"installation_id", "external_repo_id"})
        }
)
public class ConnectedRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "installation_id", nullable = false)
    private UUID installationId;

    @Column(nullable = false, length = 32)
    private String provider = "GITHUB";

    @Column(name = "external_repo_id", nullable = false, length = 64)
    private String externalRepoId;

    @Column(nullable = false)
    private String name;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "html_url", nullable = false, length = 512)
    private String htmlUrl;

    @Column(name = "default_branch", nullable = false, length = 128)
    private String defaultBranch = "main";

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ConnectedRepository() {}

    public ConnectedRepository(
            UUID installationId,
            String provider,
            String externalRepoId,
            String name,
            String fullName,
            String htmlUrl,
            String defaultBranch,
            boolean isActive
    ) {
        this.installationId = installationId;
        this.provider = provider != null ? provider : "GITHUB";
        this.externalRepoId = externalRepoId;
        this.name = name;
        this.fullName = fullName;
        this.htmlUrl = htmlUrl;
        this.defaultBranch = defaultBranch != null ? defaultBranch : "main";
        this.isActive = isActive;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getInstallationId() {
        return installationId;
    }

    public String getProvider() {
        return provider;
    }

    public String getExternalRepoId() {
        return externalRepoId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(Instant lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
