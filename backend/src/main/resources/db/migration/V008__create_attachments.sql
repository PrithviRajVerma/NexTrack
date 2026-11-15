-- ==============================================================================
-- NexTrack Schema Migration: File Attachments and Board Projection Update (V008)
-- ==============================================================================

-- 1. Create attachments table
CREATE TABLE IF NOT EXISTS attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    uploader_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(128) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ NULL,
    CONSTRAINT uq_attachments_storage_key UNIQUE (storage_key)
);

CREATE INDEX idx_attachments_issue_id ON attachments (issue_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_attachments_uploader_id ON attachments (uploader_id);

-- 2. Update v_board_cards to calculate active attachment counts
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
    0::bigint AS pr_count,
    0::bigint AS commit_count,
    0::bigint AS branch_count
FROM issues i
INNER JOIN projects p ON p.id = i.project_id
LEFT JOIN users u ON u.id = i.assignee_id
WHERE i.archived_at IS NULL;
