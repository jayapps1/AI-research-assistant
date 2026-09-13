-- ============================================================
-- AI RESEARCH ASSISTANT
-- V1 - CREATE USERS TABLE
-- ============================================================
--
-- Purpose:
-- Stores the primary identity record for every registered user
-- of the AI Research Assistant platform.
--
-- Security:
-- Plain-text passwords must never be stored in this table.
-- password_hash must contain only a secure one-way hash.
--
-- Design:
-- UUID primary keys are used so public APIs do not expose
-- predictable sequential database identifiers.
-- ============================================================


CREATE TABLE users
(
    -- ========================================================
    -- PRIMARY IDENTIFIER
    -- ========================================================

    id UUID PRIMARY KEY,


    -- ========================================================
    -- IDENTITY / AUTHENTICATION
    -- ========================================================

    -- Primary email address used for account identification.
    email VARCHAR(255) NOT NULL,

    -- Secure password hash.
    -- Nullable to support external authentication providers.
    password_hash VARCHAR(255),


    -- ========================================================
    -- PROFILE
    -- ========================================================

    first_name VARCHAR(100),

    last_name VARCHAR(100),


    -- ========================================================
    -- ACCOUNT STATE
    -- ========================================================

    status VARCHAR(30)
        NOT NULL
        DEFAULT 'ACTIVE',

    email_verified BOOLEAN
        NOT NULL
        DEFAULT FALSE,


    -- ========================================================
    -- LOCALIZATION
    -- ========================================================

    locale VARCHAR(20)
        DEFAULT 'en',


    -- ========================================================
    -- AUDIT TIMESTAMPS
    -- ========================================================

    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP,


    -- ========================================================
    -- DATA INTEGRITY
    -- ========================================================

    CONSTRAINT chk_users_status
        CHECK (
            status IN (
                'ACTIVE',
                'INACTIVE',
                'SUSPENDED',
                'PENDING'
            )
        )
);


-- ============================================================
-- CASE-INSENSITIVE EMAIL UNIQUENESS
-- ============================================================
--
-- PostgreSQL normally treats the following as different:
--
--     user@example.com
--     USER@example.com
--
-- Account identity should treat them as the same address.
-- ============================================================

CREATE UNIQUE INDEX uk_users_email_lower
    ON users (LOWER(email));


-- ============================================================
-- USER STATUS INDEX
-- ============================================================
--
-- Supports administrative queries for active, suspended,
-- inactive and pending users.
-- ============================================================

CREATE INDEX idx_users_status
    ON users(status);