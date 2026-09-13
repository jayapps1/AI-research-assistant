-- ============================================================
-- AI RESEARCH ASSISTANT
-- V4 - CREATE PASSWORD RESET AUTHORIZATIONS TABLE
-- ============================================================
--
-- Purpose:
-- Stores short-lived, single-purpose authorizations created after
-- a password-recovery factor has been verified.
--
-- Security:
-- The raw reset token is returned to the client once and is never
-- stored. token_hash contains a SHA-256 hash of a high-entropy
-- opaque token.
-- ============================================================

CREATE TABLE password_reset_authorizations
(
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,

    token_hash VARCHAR(64) NOT NULL,

    verification_method VARCHAR(30) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    expires_at TIMESTAMP WITH TIME ZONE
        NOT NULL,

    used_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_password_reset_authorizations_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT uk_password_reset_authorizations_token_hash
        UNIQUE (token_hash),

    CONSTRAINT chk_password_reset_authorizations_method
        CHECK (
            verification_method IN (
                'TOTP',
                'EMAIL',
                'RECOVERY_CODE',
                'SMS'
            )
        ),

    CONSTRAINT chk_password_reset_authorizations_expiry
        CHECK (expires_at > created_at)
);

CREATE INDEX idx_password_reset_authorizations_user_id
    ON password_reset_authorizations(user_id);

CREATE INDEX idx_password_reset_authorizations_expires_at
    ON password_reset_authorizations(expires_at);
