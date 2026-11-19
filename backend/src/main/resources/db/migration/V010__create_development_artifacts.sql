-- ==============================================================================
-- NexTrack Schema Migration: Development Artifacts & Traceability (V010)
-- ==============================================================================

-- 1. Development branches table
CREATE TABLE IF NOT EXISTS development_branches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id UUID NOT NULL REFERENCES connected_repositories(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    last_commit_sha VARCHAR(64) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_branch_repo_name UNIQUE (repository_id, name)
);

CREATE INDEX IF NOT EXISTS idx_development_branches_repo ON development_branches (repository_id);

-- 2. Development commits table
CREATE TABLE IF NOT EXISTS development_commits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id UUID NOT NULL REFERENCES connected_repositories(id) ON DELETE CASCADE,
    commit_sha VARCHAR(64) NOT NULL,
    message TEXT NOT NULL,
    author_name VARCHAR(255) NOT NULL,
    author_email VARCHAR(255) NOT NULL,
    commit_url VARCHAR(512) NOT NULL,
    committed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_commit_repo_sha UNIQUE (repository_id, commit_sha)
);

CREATE INDEX IF NOT EXISTS idx_development_commits_repo ON development_commits (repository_id);
CREATE INDEX IF NOT EXISTS idx_development_commits_sha ON development_commits (commit_sha);
CREATE INDEX IF NOT EXISTS idx_development_commits_committed_at ON development_commits (committed_at DESC);

-- 3. Pull requests table
CREATE TABLE IF NOT EXISTS pull_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id UUID NOT NULL REFERENCES connected_repositories(id) ON DELETE CASCADE,
    provider_pr_id BIGINT NOT NULL,
    pr_number INT NOT NULL,
    title VARCHAR(512) NOT NULL,
    description TEXT NULL,
    source_branch VARCHAR(255) NOT NULL,
    target_branch VARCHAR(255) NOT NULL,
    pr_url VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    review_status VARCHAR(32) NULL,
    is_draft BOOLEAN NOT NULL DEFAULT false,
    merged_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_pr_repo_num UNIQUE (repository_id, pr_number),
    CONSTRAINT uq_pr_repo_provider_id UNIQUE (repository_id, provider_pr_id)
);

CREATE INDEX IF NOT EXISTS idx_pull_requests_repo ON pull_requests (repository_id);
CREATE INDEX IF NOT EXISTS idx_pull_requests_status ON pull_requests (status);

-- 4. Issue-to-Branch association table (M:N)
CREATE TABLE IF NOT EXISTS issue_development_branches (
    issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES development_branches(id) ON DELETE CASCADE,
    link_type VARCHAR(32) NOT NULL DEFAULT 'AUTO_EXTRACTED',
    linked_by UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    linked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (issue_id, branch_id)
);

CREATE INDEX IF NOT EXISTS idx_issue_dev_branches_branch ON issue_development_branches (branch_id);

-- 5. Issue-to-Commit association table (M:N)
CREATE TABLE IF NOT EXISTS issue_development_commits (
    issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    commit_id UUID NOT NULL REFERENCES development_commits(id) ON DELETE CASCADE,
    link_type VARCHAR(32) NOT NULL DEFAULT 'AUTO_EXTRACTED',
    linked_by UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    linked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (issue_id, commit_id)
);

CREATE INDEX IF NOT EXISTS idx_issue_dev_commits_commit ON issue_development_commits (commit_id);

-- 6. Issue-to-PullRequest association table (M:N)
CREATE TABLE IF NOT EXISTS issue_pull_requests (
    issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    pull_request_id UUID NOT NULL REFERENCES pull_requests(id) ON DELETE CASCADE,
    link_type VARCHAR(32) NOT NULL DEFAULT 'AUTO_EXTRACTED',
    linked_by UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    linked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (issue_id, pull_request_id)
);

CREATE INDEX IF NOT EXISTS idx_issue_pull_requests_pr ON issue_pull_requests (pull_request_id);

-- 7. Update read-optimized board projection view with live traceability counts
CREATE OR REPLACE VIEW v_board_cards AS
SELECT 
    i.id,
    i.project_id,
    p.project_key,
    i.issue_number,
    p.project_key || '-' || i.issue_number AS formatted_key,
    i.title,
    i.status,
    i.priority,
    i.issue_type,
    i.assignee_id,
    u.display_name AS assignee_name,
    u.email AS assignee_email,
    i.due_date,
    i.story_points,
    i.version,
    i.created_at,
    i.updated_at,
    (SELECT COUNT(*) FROM comments c WHERE c.issue_id = i.id AND c.deleted_at IS NULL) AS comment_count,
    (SELECT COUNT(*) FROM attachments a WHERE a.issue_id = i.id AND a.deleted_at IS NULL) AS attachment_count,
    (SELECT COUNT(*) FROM issue_pull_requests ipr WHERE ipr.issue_id = i.id) AS pr_count,
    (SELECT COUNT(*) FROM issue_development_commits idc WHERE idc.issue_id = i.id) AS commit_count,
    (SELECT COUNT(*) FROM issue_development_branches idb WHERE idb.issue_id = i.id) AS branch_count
FROM issues i
INNER JOIN projects p ON p.id = i.project_id
LEFT JOIN users u ON u.id = i.assignee_id
WHERE i.archived_at IS NULL;
