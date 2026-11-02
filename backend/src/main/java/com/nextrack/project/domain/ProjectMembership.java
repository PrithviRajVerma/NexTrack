package com.nextrack.project.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "project_memberships")
@IdClass(ProjectMembership.ProjectMembershipId.class)
public class ProjectMembership {

    @Id
    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectRole role = ProjectRole.CONTRIBUTOR;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt = Instant.now();

    protected ProjectMembership() {}

    public ProjectMembership(UUID projectId, UUID userId, ProjectRole role) {
        this.projectId = projectId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = Instant.now();
    }

    public UUID getProjectId() { return projectId; }
    public UUID getUserId() { return userId; }
    public ProjectRole getRole() { return role; }
    public Instant getJoinedAt() { return joinedAt; }

    public void changeRole(ProjectRole role) {
        this.role = role;
    }

    public static class ProjectMembershipId implements Serializable {
        private UUID projectId;
        private UUID userId;

        public ProjectMembershipId() {}
        public ProjectMembershipId(UUID projectId, UUID userId) {
            this.projectId = projectId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProjectMembershipId that)) return false;
            return Objects.equals(projectId, that.projectId) && Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(projectId, userId);
        }
    }
}
