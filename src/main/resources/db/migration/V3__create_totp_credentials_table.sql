-- ============================================================
-- AI RESEARCH ASSISTANT
-- V3 - CREATE TOTP CREDENTIALS TABLE
-- ============================================================
--
-- Purpose:
-- Stores authenticator-app TOTP credential metadata for users
-- who enroll a standards-based RFC 6238 authenticator.
--
-- Security:
-- TOTP secrets must be recoverable by the server for future code
-- verification, so they cannot be hashed like passwords. The
-- encrypted_secret column stores only encrypted ciphertext produced
-- by application credential encryption. Plain-text TOTP secrets
-- must never be persisted.
--
-- Replay protection:
-- last_used_timestep records the most recent accepted TOTP time
-- step. Verification locks the row and advances this value
-- atomically so the same code window cannot be reused by concurrent
-- login attempts.
--
-- Deletion policy:
-- The user foreign key deliberately uses restrictive behavior.
-- TOTP credentials are security records and should be disabled or
-- rotated through explicit security workflows.
-- ============================================================

CREATE TABLE totp_credentials
(
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,

    encrypted_secret TEXT NOT NULL,

    enabled BOOLEAN
        NOT NULL
        DEFAULT FALSE,

    verified_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    last_used_timestep BIGINT,

    CONSTRAINT fk_totp_credentials_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT uk_totp_credentials_user_id
        UNIQUE (user_id),

    CONSTRAINT chk_totp_credentials_verified_when_enabled
        CHECK (
            enabled = FALSE
            OR verified_at IS NOT NULL
        )
);

CREATE INDEX idx_totp_credentials_enabled
    ON totp_credentials(enabled);
