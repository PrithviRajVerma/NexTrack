package com.nextrack.workspace.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workspaces")
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "archived_at")
    private Instant archivedAt;

    protected Workspace() {}

    public Workspace(String name, String slug, UUID ownerUserId) {
        this.name = name.trim();
        this.slug = slug.toLowerCase().trim();
        this.ownerUserId = ownerUserId;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getArchivedAt() { return archivedAt; }

    public void update(String name) {
        this.name = name.trim();
    }

    public void archive() {
        this.archivedAt = Instant.now();
    }
}
