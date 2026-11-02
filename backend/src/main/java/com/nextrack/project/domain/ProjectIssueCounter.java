package com.nextrack.project.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "project_issue_counters")
public class ProjectIssueCounter {

    @Id
    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "last_issue_number", nullable = false)
    private long lastIssueNumber = 0L;

    protected ProjectIssueCounter() {}

    public ProjectIssueCounter(UUID projectId) {
        this.projectId = projectId;
        this.lastIssueNumber = 0L;
    }

    public UUID getProjectId() { return projectId; }
    public long getLastIssueNumber() { return lastIssueNumber; }
}
