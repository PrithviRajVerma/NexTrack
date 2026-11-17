package com.nextrack.development.domain.repository;

import com.nextrack.development.domain.entity.ProjectConnectedRepository;
import com.nextrack.development.domain.entity.ProjectConnectedRepositoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectConnectedRepositoryRepository extends JpaRepository<ProjectConnectedRepository, ProjectConnectedRepositoryId> {

    List<ProjectConnectedRepository> findAllByProjectId(UUID projectId);

    List<ProjectConnectedRepository> findAllByRepositoryId(UUID repositoryId);

    boolean existsByProjectIdAndRepositoryId(UUID projectId, UUID repositoryId);

    void deleteByProjectIdAndRepositoryId(UUID projectId, UUID repositoryId);
}
