-- ==============================================================================
-- NexTrack Schema Migration: Read-Optimized Board Projection View (V006)
-- ==============================================================================

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
    0::bigint AS attachment_count,
    0::bigint AS pr_count,
    0::bigint AS commit_count,
    0::bigint AS branch_count
FROM issues i
INNER JOIN projects p ON p.id = i.project_id
LEFT JOIN users u ON u.id = i.assignee_id
WHERE i.archived_at IS NULL;
