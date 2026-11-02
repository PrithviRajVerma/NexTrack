package com.nextrack.workspace.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
    Optional<Workspace> findBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCase(String slug);
}
