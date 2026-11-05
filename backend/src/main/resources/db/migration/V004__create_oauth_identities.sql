-- ==============================================================================
-- NexTrack Schema Migration: Federated OAuth2 Identities & Account Linking (V004)
-- ==============================================================================

-- 1. Allow nullable password_hash for OAuth2 users (no local password)
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

-- 2. Add avatar_url to users table for social profile pictures
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(512) NULL;

-- 3. Create user_oauth_identities table for multi-provider federated login
CREATE TABLE IF NOT EXISTS user_oauth_identities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL,
    provider_user_id VARCHAR(128) NOT NULL,
    provider_email VARCHAR(255) NULL,
    email_verified BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_oauth_provider_user UNIQUE (provider, provider_user_id)
);

-- 4. Create composite and foreign key indexes
CREATE INDEX IF NOT EXISTS idx_oauth_identities_user_id ON user_oauth_identities(user_id);
CREATE INDEX IF NOT EXISTS idx_oauth_identities_provider_lookup ON user_oauth_identities(provider, provider_user_id);
