-- ==============================================================================
-- NexTrack Schema Migration: GitHub App Integration & Webhook Inbox Queue (V009)
-- ==============================================================================

-- 1. Integration installations table (Workspace-level GitHub App installations)
CREATE TABLE IF NOT EXISTS integration_installations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL DEFAULT 'GITHUB',
    installation_id BIGINT NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    repository_selection VARCHAR(32) NULL,
    permissions JSONB NULL,
    encrypted_webhook_secret TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_integration_installation UNIQUE (provider, installation_id)
);

CREATE INDEX IF NOT EXISTS idx_integration_installations_workspace 
    ON integration_installations (workspace_id);

-- 2. Connected repositories table (Repositories accessible via installation)
CREATE TABLE IF NOT EXISTS connected_repositories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    installation_id UUID NOT NULL REFERENCES integration_installations(id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL DEFAULT 'GITHUB',
    external_repo_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    html_url VARCHAR(512) NOT NULL,
    default_branch VARCHAR(128) NOT NULL DEFAULT 'main',
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_synced_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_connected_repo UNIQUE (installation_id, external_repo_id)
);

CREATE INDEX IF NOT EXISTS idx_connected_repositories_installation 
    ON connected_repositories (installation_id);

-- 3. Project-to-repository join table (Explicit project associations)
CREATE TABLE IF NOT EXISTS project_connected_repositories (
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    repository_id UUID NOT NULL REFERENCES connected_repositories(id) ON DELETE CASCADE,
    connected_by UUID NOT NULL REFERENCES users(id),
    connected_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (project_id, repository_id)
);

-- 4. Webhook inbox events table (Idempotent 2-phase claim/lease queue)
CREATE TABLE IF NOT EXISTS webhook_inbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider VARCHAR(32) NOT NULL DEFAULT 'GITHUB',
    delivery_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    lease_owner_token UUID NULL,
    processing_lease_expires_at TIMESTAMPTZ NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ NULL,
    error_message TEXT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ NULL,
    CONSTRAINT uq_webhook_provider_delivery UNIQUE (provider, delivery_id)
);

-- Partial index for high-throughput FIFO worker polling with SKIP LOCKED
CREATE INDEX IF NOT EXISTS idx_webhook_inbox_pending 
    ON webhook_inbox_events (status, next_retry_at, received_at ASC) 
    WHERE status = 'PENDING';

-- Partial index for supervisor scans of stuck processing leases
CREATE INDEX IF NOT EXISTS idx_webhook_inbox_lease 
    ON webhook_inbox_events (status, processing_lease_expires_at) 
    WHERE status = 'PROCESSING';
