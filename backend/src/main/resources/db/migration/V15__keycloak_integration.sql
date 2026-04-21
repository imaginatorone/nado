-- V15: Keycloak integration fields + MODERATOR role + trust score preparation
-- Adds keycloak user linking, auth provider tracking, and trust-related fields

ALTER TABLE users ADD COLUMN IF NOT EXISTS keycloak_user_id VARCHAR(255) UNIQUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_verified_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS trust_score DOUBLE PRECISION;
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_banned BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS successful_deals INTEGER NOT NULL DEFAULT 0;

-- Update role column to support MODERATOR
-- No constraint change needed — role is stored as VARCHAR via @Enumerated(EnumType.STRING)

-- Index for Keycloak user lookup
CREATE INDEX idx_users_keycloak_user_id ON users(keycloak_user_id) WHERE keycloak_user_id IS NOT NULL;
