-- ============================================================
-- AI RESEARCH ASSISTANT
-- V21 - PAYMENT RECOVERY, COMPLIMENTARY ACCESS, JOBS, FILE SECURITY
-- ============================================================

ALTER TABLE workspace_subscriptions
    ADD COLUMN access_source VARCHAR(30) NOT NULL DEFAULT 'FREE_DEFAULT';

ALTER TABLE document_versions
    ADD COLUMN scan_status VARCHAR(30) NOT NULL DEFAULT 'NOT_SCANNED',
    ADD COLUMN quarantined BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE billing_payment_intents (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    initiated_by UUID NOT NULL REFERENCES users(id),
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    billing_interval VARCHAR(20) NOT NULL,
    expected_amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(40) NOT NULL,
    next_attempt_number INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    settled_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_billing_payment_intents_amount CHECK (expected_amount >= 0),
    CONSTRAINT chk_billing_payment_intents_next_attempt CHECK (next_attempt_number >= 1),
    CONSTRAINT chk_billing_payment_intents_status CHECK (status IN ('OPEN','PAYMENT_PENDING','PAID','CANCELLED','EXPIRED','REQUIRES_REVIEW'))
);
CREATE INDEX idx_billing_payment_intents_workspace_created ON billing_payment_intents(workspace_id, created_at);
CREATE INDEX idx_billing_payment_intents_status ON billing_payment_intents(status);

CREATE TABLE payment_attempts (
    id UUID PRIMARY KEY,
    payment_intent_id UUID NOT NULL REFERENCES billing_payment_intents(id),
    provider VARCHAR(40) NOT NULL,
    environment VARCHAR(20) NOT NULL,
    internal_reference VARCHAR(80) NOT NULL,
    provider_reference VARCHAR(255),
    channel VARCHAR(30),
    status VARCHAR(40) NOT NULL,
    expected_amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    attempt_number INTEGER NOT NULL,
    provider_status VARCHAR(80),
    failure_code VARCHAR(100),
    failure_message_safe VARCHAR(500),
    initialized_at TIMESTAMP WITH TIME ZONE,
    provider_verified_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    authorization_url VARCHAR(1000),
    access_code VARCHAR(255),
    requires_review BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_payment_attempts_internal_reference UNIQUE (internal_reference),
    CONSTRAINT uk_payment_attempts_provider_reference UNIQUE (provider, environment, provider_reference),
    CONSTRAINT uk_payment_attempts_intent_attempt UNIQUE (payment_intent_id, attempt_number),
    CONSTRAINT chk_payment_attempts_amount CHECK (expected_amount >= 0),
    CONSTRAINT chk_payment_attempts_attempt_number CHECK (attempt_number >= 1),
    CONSTRAINT chk_payment_attempts_status CHECK (status IN ('CREATED','INITIALIZED','PENDING','PROCESSING','SUCCESS','FAILED','ABANDONED','CANCELLED','EXPIRED','VERIFICATION_FAILED'))
);
CREATE INDEX idx_payment_attempts_intent ON payment_attempts(payment_intent_id, attempt_number);
CREATE INDEX idx_payment_attempts_status ON payment_attempts(status);

CREATE TABLE payment_idempotency_records (
    id UUID PRIMARY KEY,
    scope VARCHAR(80) NOT NULL,
    idempotency_key VARCHAR(180) NOT NULL,
    user_id UUID REFERENCES users(id),
    payment_intent_id UUID,
    payment_attempt_id UUID,
    response_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_payment_idempotency_scope_key UNIQUE (scope, idempotency_key)
);

CREATE TABLE complimentary_access_grants (
    id UUID PRIMARY KEY,
    scope VARCHAR(20) NOT NULL,
    user_id UUID REFERENCES users(id),
    workspace_id UUID REFERENCES workspaces(id),
    plan_id UUID REFERENCES subscription_plans(id),
    type VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    granted_by UUID NOT NULL REFERENCES users(id),
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoked_by UUID REFERENCES users(id),
    revocation_reason VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_complimentary_grants_target CHECK (user_id IS NOT NULL OR workspace_id IS NOT NULL),
    CONSTRAINT chk_complimentary_grants_validity CHECK (expires_at IS NULL OR expires_at > starts_at)
);
CREATE INDEX idx_complimentary_grants_workspace_status ON complimentary_access_grants(workspace_id, status);
CREATE INDEX idx_complimentary_grants_user_status ON complimentary_access_grants(user_id, status);
CREATE INDEX idx_complimentary_grants_validity ON complimentary_access_grants(starts_at, expires_at);

CREATE TABLE entitlement_overrides (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    feature VARCHAR(80) NOT NULL,
    limit_mode VARCHAR(20) NOT NULL,
    limit_value BIGINT,
    limit_unit VARCHAR(30),
    status VARCHAR(30) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    granted_by UUID NOT NULL REFERENCES users(id),
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_entitlement_overrides_limit CHECK (
        (limit_mode = 'LIMITED' AND limit_value IS NOT NULL AND limit_value >= 0)
        OR (limit_mode IN ('UNLIMITED','DISABLED') AND limit_value IS NULL)
    )
);
CREATE INDEX idx_entitlement_overrides_workspace_feature ON entitlement_overrides(workspace_id, feature, status);

CREATE TABLE background_jobs (
    id UUID PRIMARY KEY,
    type VARCHAR(60) NOT NULL,
    status VARCHAR(40) NOT NULL,
    workspace_id UUID REFERENCES workspaces(id),
    project_id UUID REFERENCES research_projects(id) ON DELETE SET NULL,
    requested_by UUID REFERENCES users(id),
    source_type VARCHAR(80),
    source_id UUID,
    attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    progress_percent INTEGER,
    failure_code VARCHAR(100),
    failure_message_safe VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    next_attempt_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_background_jobs_attempts CHECK (attempts >= 0 AND max_attempts >= 1),
    CONSTRAINT chk_background_jobs_progress CHECK (progress_percent IS NULL OR progress_percent BETWEEN 0 AND 100)
);
CREATE INDEX idx_background_jobs_status_next ON background_jobs(status, next_attempt_at);
CREATE INDEX idx_background_jobs_workspace ON background_jobs(workspace_id, created_at);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    aggregate_type VARCHAR(120) NOT NULL,
    aggregate_id UUID NOT NULL,
    payload_json TEXT,
    payload_version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    attempts INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_outbox_events_attempts CHECK (attempts >= 0)
);
CREATE INDEX idx_outbox_events_unprocessed ON outbox_events(processed_at, created_at);
