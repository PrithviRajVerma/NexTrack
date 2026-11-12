-- ==============================================================================
-- NexTrack Schema Migration: In-App Notifications & Transactional Outbox (V007)
-- ==============================================================================

-- 1. Create outbox_events table for transactional outbox pattern
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ NULL,
    error_message TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ NULL
);

-- Partial index for high-throughput polling with SKIP LOCKED
CREATE INDEX IF NOT EXISTS idx_outbox_events_pending 
    ON outbox_events (status, next_retry_at, created_at ASC) 
    WHERE status = 'PENDING';

-- 2. Create notifications table for recipient-specific user notifications
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_type VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    dedup_key VARCHAR(255) NOT NULL,
    read_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_notifications_dedup_key UNIQUE (dedup_key)
);

-- Composite index for user unread notification retrieval
CREATE INDEX IF NOT EXISTS idx_notifications_recipient 
    ON notifications (recipient_id, read_at, created_at DESC);
