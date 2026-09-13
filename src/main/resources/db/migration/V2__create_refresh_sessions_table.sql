-- ============================================================
-- AI RESEARCH ASSISTANT
-- V2 - CREATE REFRESH SESSIONS TABLE
-- ============================================================
--
-- Purpose:
-- Stores server-side refresh-session records used to rotate
-- opaque refresh tokens.
--
-- Security:
-- Raw refresh tokens are returned to the client once and are
-- never stored. token_hash contains a SHA-256 hash of a
-- high-entropy random token, allowing lookup without keeping
-- bearer credentials in PostgreSQL.
--
-- Deletion policy:
-- The user foreign key deliberately uses the default restrictive
-- behavior. Refresh sessions are security/audit records and
-- should not disappear automatically through casual user-row
-- deletion.
-- ============================================================

CREATE TABLE refresh_sessions
(
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,

    token_hash VARCHAR(64) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    expires_at TIMESTAMP WITH TIME ZONE
        NOT NULL,

    last_used_at TIMESTAMP WITH TIME ZONE,

    revoked_at TIMESTAMP WITH TIME ZONE,

    replaced_by_session_id UUID,

    user_agent VARCHAR(512),

    ip_address VARCHAR(64),

    CONSTRAINT fk_refresh_sessions_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_refresh_sessions_replacement
        FOREIGN KEY (replaced_by_session_id)
        REFERENCES refresh_sessions(id),

    CONSTRAINT uk_refresh_sessions_token_hash
        UNIQUE (token_hash),

    CONSTRAINT chk_refresh_sessions_expiry
        CHECK (expires_at > created_at)
);

CREATE INDEX idx_refresh_sessions_user_id
    ON refresh_sessions(user_id);

CREATE INDEX idx_refresh_sessions_expires_at
    ON refresh_sessions(expires_at);
