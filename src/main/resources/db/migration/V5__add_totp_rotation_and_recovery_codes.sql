-- ============================================================
-- AI RESEARCH ASSISTANT
-- V5 - TOTP ROTATION AND RECOVERY CODES
-- ============================================================
--
-- Purpose:
-- Adds safe pending-secret rotation support to the single
-- totp_credentials row and creates one-time recovery codes.
--
-- Security:
-- Recovery codes are shown to the user once, then stored only as
-- slow password-style hashes. They are not TOTP secrets and must
-- not be used as normal login passwords.
-- ============================================================

ALTER TABLE totp_credentials
    ADD COLUMN pending_encrypted_secret TEXT,
    ADD COLUMN pending_created_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN disabled_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE totp_recovery_codes
(
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,

    code_hash VARCHAR(255) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    used_at TIMESTAMP WITH TIME ZONE,

    revoked_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_totp_recovery_codes_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);

CREATE INDEX idx_totp_recovery_codes_user_id
    ON totp_recovery_codes(user_id);

CREATE INDEX idx_totp_recovery_codes_available
    ON totp_recovery_codes(user_id, used_at, revoked_at);
