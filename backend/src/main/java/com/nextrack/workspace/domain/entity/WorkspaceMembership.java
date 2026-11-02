package com.nextrack.workspace.domain.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "workspace_memberships")
@IdClass(WorkspaceMembership.WorkspaceMembershipId.class)
public class WorkspaceMembership {

    @Id
    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkspaceRole role = WorkspaceRole.MEMBER;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt = Instant.now();

    protected WorkspaceMembership() {}

    public WorkspaceMembership(UUID workspaceId, UUID userId, WorkspaceRole role) {
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = Instant.now();
    }

    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getUserId() { return userId; }
    public WorkspaceRole getRole() { return role; }
    public Instant getJoinedAt() { return joinedAt; }

    public void changeRole(WorkspaceRole role) {
        this.role = role;
    }

    public static class WorkspaceMembershipId implements Serializable {
        private UUID workspaceId;
        private UUID userId;

        public WorkspaceMembershipId() {}
        public WorkspaceMembershipId(UUID workspaceId, UUID userId) {
            this.workspaceId = workspaceId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof WorkspaceMembershipId that)) return false;
            return Objects.equals(workspaceId, that.workspaceId) && Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(workspaceId, userId);
        }
    }
}
