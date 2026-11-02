package com.nextrack.project.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMembershipRepository extends JpaRepository<ProjectMembership, ProjectMembership.ProjectMembershipId> {
    List<ProjectMembership> findByProjectId(UUID projectId);
    List<ProjectMembership> findByUserId(UUID userId);
    Optional<ProjectMembership> findByProjectIdAndUserId(UUID projectId, UUID userId);
    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);
}
