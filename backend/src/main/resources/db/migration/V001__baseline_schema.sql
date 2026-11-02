-- ==============================================================================
-- NexTrack Baseline Database Schema Migration (V001)
-- ==============================================================================

-- Enable pgcrypto extension for cryptographic functions if needed
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- System metadata table to verify database connectivity and migration pipeline
CREATE TABLE IF NOT EXISTS system_metadata (
    key VARCHAR(64) PRIMARY KEY,
    value VARCHAR(255) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO system_metadata (key, value)
VALUES ('schema_version', '1.0.0')
ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value, updated_at = CURRENT_TIMESTAMP;
