package com.nextrack.project.domain.repository;

import com.nextrack.project.domain.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByWorkspaceId(UUID workspaceId);
    Optional<Project> findByWorkspaceIdAndProjectKeyIgnoreCase(UUID workspaceId, String projectKey);
    boolean existsByWorkspaceIdAndProjectKeyIgnoreCase(UUID workspaceId, String projectKey);
}
