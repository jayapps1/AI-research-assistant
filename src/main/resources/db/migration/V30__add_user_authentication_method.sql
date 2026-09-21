-- ============================================================
-- AI RESEARCH ASSISTANT
-- V30 - USER AUTHENTICATION METHOD (PASSWORD_OR_TOTP SUPPORT)
-- ============================================================

ALTER TABLE users
    ADD COLUMN authentication_method VARCHAR(32) NOT NULL DEFAULT 'PASSWORD';

-- Existing users who already have enabled TOTP credentials
-- default to the flexible alternative-method mode (PASSWORD_OR_TOTP)
UPDATE users
SET authentication_method = 'PASSWORD_OR_TOTP'
WHERE id IN (
    SELECT user_id FROM totp_credentials WHERE enabled = TRUE
);
