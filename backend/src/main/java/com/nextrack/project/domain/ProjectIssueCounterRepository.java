package com.nextrack.project.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProjectIssueCounterRepository extends JpaRepository<ProjectIssueCounter, UUID> {

    @Query(value = """
        UPDATE project_issue_counters
        SET last_issue_number = last_issue_number + 1
        WHERE project_id = :projectId
        RETURNING last_issue_number
    """, nativeQuery = true)
    long incrementAndGet(@Param("projectId") UUID projectId);
}
