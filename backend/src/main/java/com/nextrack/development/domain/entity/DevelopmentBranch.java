package com.nextrack.development.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "development_branches", uniqueConstraints = {
        @UniqueConstraint(name = "uq_branch_repo_name", columnNames = {"repository_id", "name"})
})
public class DevelopmentBranch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "repository_id", nullable = false)
    private UUID repositoryId;

    @Column(nullable = false)
    private String name;

    @Column(name = "last_commit_sha", length = 64)
    private String lastCommitSha;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected DevelopmentBranch() {}

    public DevelopmentBranch(UUID repositoryId, String name, String lastCommitSha) {
        this.repositoryId = repositoryId;
        this.name = name;
        this.lastCommitSha = lastCommitSha;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getRepositoryId() { return repositoryId; }
    public String getName() { return name; }
    public String getLastCommitSha() { return lastCommitSha; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void updateLastCommitSha(String lastCommitSha) {
        this.lastCommitSha = lastCommitSha;
        this.updatedAt = Instant.now();
    }
}
