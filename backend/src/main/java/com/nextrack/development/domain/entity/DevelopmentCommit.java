package com.nextrack.development.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "development_commits", uniqueConstraints = {
        @UniqueConstraint(name = "uq_commit_repo_sha", columnNames = {"repository_id", "commit_sha"})
})
public class DevelopmentCommit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "repository_id", nullable = false)
    private UUID repositoryId;

    @Column(name = "commit_sha", nullable = false, length = 64)
    private String commitSha;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "author_name", nullable = false)
    private String authorName;

    @Column(name = "author_email", nullable = false)
    private String authorEmail;

    @Column(name = "commit_url", nullable = false, length = 512)
    private String commitUrl;

    @Column(name = "committed_at", nullable = false)
    private Instant committedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected DevelopmentCommit() {}

    public DevelopmentCommit(
            UUID repositoryId,
            String commitSha,
            String message,
            String authorName,
            String authorEmail,
            String commitUrl,
            Instant committedAt
    ) {
        this.repositoryId = repositoryId;
        this.commitSha = commitSha;
        this.message = message;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.commitUrl = commitUrl;
        this.committedAt = committedAt != null ? committedAt : Instant.now();
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getRepositoryId() { return repositoryId; }
    public String getCommitSha() { return commitSha; }
    public String getMessage() { return message; }
    public String getAuthorName() { return authorName; }
    public String getAuthorEmail() { return authorEmail; }
    public String getCommitUrl() { return commitUrl; }
    public Instant getCommittedAt() { return committedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
