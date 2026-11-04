-- ==============================================================================
-- NexTrack Schema Migration: Issues Domain & Atomic Counters (V003)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS issues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    issue_number BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    issue_type VARCHAR(32) NOT NULL DEFAULT 'TASK',
    status VARCHAR(32) NOT NULL DEFAULT 'BACKLOG',
    priority VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    reporter_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    assignee_id UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    due_date TIMESTAMPTZ NULL,
    story_points INT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archived_at TIMESTAMPTZ NULL,
    CONSTRAINT uq_issues_project_issue_number UNIQUE (project_id, issue_number)
);

CREATE INDEX idx_issues_project_status ON issues (project_id, status, updated_at DESC);
CREATE INDEX idx_issues_assignee ON issues (project_id, assignee_id);
CREATE INDEX idx_issues_project_number ON issues (project_id, issue_number);
