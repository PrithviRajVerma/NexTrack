-- ==============================================================================
-- NexTrack Schema Migration: Workflow Comments and Audit Events (V005)
-- ==============================================================================

-- 1. Create comments table for issues
CREATE TABLE IF NOT EXISTS comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ NULL
);

CREATE INDEX IF NOT EXISTS idx_comments_issue_id ON comments (issue_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_comments_author_id ON comments (author_id);

-- 2. Create audit_events table for system and issue audit trails
CREATE TABLE IF NOT EXISTS audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(64) NOT NULL,
    before_payload JSONB NULL,
    after_payload JSONB NULL,
    trace_id VARCHAR(128) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_events_entity ON audit_events (entity_type, entity_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_events_actor ON audit_events (actor_id, created_at DESC);
