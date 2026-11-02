package com.nextrack.workspace.domain.repository;

import com.nextrack.workspace.domain.entity.WorkspaceMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceMembershipRepository extends JpaRepository<WorkspaceMembership, WorkspaceMembership.WorkspaceMembershipId> {
    List<WorkspaceMembership> findByUserId(UUID userId);
    List<WorkspaceMembership> findByWorkspaceId(UUID workspaceId);
    Optional<WorkspaceMembership> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);
    boolean existsByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);
}
