-- ============================================================
-- AI RESEARCH ASSISTANT
-- V20 - SAAS SUBSCRIPTIONS, USAGE, BILLING, NOTIFICATIONS, ADMIN AUDIT
-- ============================================================

CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY,
    code VARCHAR(60) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL,
    billing_interval VARCHAR(20) NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    publicly_available BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_subscription_plans_code UNIQUE (code),
    CONSTRAINT chk_subscription_plans_status CHECK (status IN ('ACTIVE','INACTIVE','ARCHIVED')),
    CONSTRAINT chk_subscription_plans_interval CHECK (billing_interval IN ('NONE','MONTHLY','YEARLY')),
    CONSTRAINT chk_subscription_plans_price CHECK (price >= 0)
);
CREATE INDEX idx_subscription_plans_status ON subscription_plans(status);
CREATE INDEX idx_subscription_plans_public_order ON subscription_plans(publicly_available, display_order);

CREATE TABLE plan_entitlements (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL REFERENCES subscription_plans(id) ON DELETE CASCADE,
    feature VARCHAR(80) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    limit_value BIGINT,
    limit_unit VARCHAR(30),
    metadata_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_plan_entitlements_plan_feature UNIQUE (plan_id, feature),
    CONSTRAINT chk_plan_entitlements_limit CHECK (limit_value IS NULL OR limit_value >= 0)
);
CREATE INDEX idx_plan_entitlements_plan ON plan_entitlements(plan_id);

CREATE TABLE workspace_subscriptions (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    status VARCHAR(30) NOT NULL,
    billing_interval VARCHAR(20) NOT NULL,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    current_period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    current_period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    cancel_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    auto_renew BOOLEAN NOT NULL DEFAULT FALSE,
    external_subscription_reference VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_workspace_subscriptions_status CHECK (status IN ('TRIALING','ACTIVE','PAST_DUE','CANCELLED','EXPIRED','SUSPENDED')),
    CONSTRAINT chk_workspace_subscriptions_interval CHECK (billing_interval IN ('NONE','MONTHLY','YEARLY')),
    CONSTRAINT chk_workspace_subscriptions_period CHECK (current_period_end > current_period_start)
);
CREATE INDEX idx_workspace_subscriptions_workspace_status ON workspace_subscriptions(workspace_id, status);
CREATE INDEX idx_workspace_subscriptions_period ON workspace_subscriptions(current_period_start, current_period_end);
CREATE UNIQUE INDEX uk_workspace_subscriptions_one_current ON workspace_subscriptions(workspace_id)
    WHERE status IN ('TRIALING','ACTIVE','PAST_DUE','SUSPENDED');

CREATE TABLE workspace_subscription_history (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    subscription_id UUID REFERENCES workspace_subscriptions(id),
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to TIMESTAMP WITH TIME ZONE,
    reason VARCHAR(80) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_workspace_subscription_history_workspace_time ON workspace_subscription_history(workspace_id, effective_from, effective_to);
CREATE INDEX idx_workspace_subscription_history_subscription ON workspace_subscription_history(subscription_id);

CREATE TABLE usage_ledger_entries (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    user_id UUID REFERENCES users(id),
    project_id UUID REFERENCES research_projects(id) ON DELETE SET NULL,
    metric VARCHAR(80) NOT NULL,
    quantity BIGINT NOT NULL,
    source_type VARCHAR(100) NOT NULL,
    source_id UUID,
    idempotency_key VARCHAR(180),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_usage_ledger_quantity CHECK (quantity >= 0),
    CONSTRAINT uk_usage_ledger_idempotency UNIQUE (idempotency_key)
);
CREATE INDEX idx_usage_ledger_workspace_metric_time ON usage_ledger_entries(workspace_id, metric, occurred_at);

CREATE TABLE usage_reservations (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    metric VARCHAR(80) NOT NULL,
    quantity BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    idempotency_key VARCHAR(180) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_usage_reservations_quantity CHECK (quantity >= 0),
    CONSTRAINT uk_usage_reservations_idempotency UNIQUE (idempotency_key)
);
CREATE INDEX idx_usage_reservations_workspace_metric_status ON usage_reservations(workspace_id, metric, status);

CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    initiated_by UUID NOT NULL REFERENCES users(id),
    provider VARCHAR(40) NOT NULL,
    environment VARCHAR(20) NOT NULL,
    provider_reference VARCHAR(255),
    internal_reference VARCHAR(80) NOT NULL,
    type VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    plan_id UUID,
    plan_code VARCHAR(60),
    billing_interval VARCHAR(20),
    authorization_url VARCHAR(1000),
    access_code VARCHAR(255),
    initiated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    verified_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    failure_code VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_payment_amount CHECK (amount >= 0),
    CONSTRAINT uk_payment_transactions_internal_reference UNIQUE (internal_reference),
    CONSTRAINT uk_payment_transactions_provider_reference UNIQUE (provider, environment, provider_reference)
);
CREATE INDEX idx_payment_transactions_workspace_created ON payment_transactions(workspace_id, created_at);
CREATE INDEX idx_payment_transactions_status ON payment_transactions(status);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    recipient_id UUID NOT NULL REFERENCES users(id),
    workspace_id UUID REFERENCES workspaces(id),
    project_id UUID REFERENCES research_projects(id) ON DELETE SET NULL,
    type VARCHAR(80) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    action_url VARCHAR(1000),
    priority VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_notifications_recipient_created ON notifications(recipient_id, created_at);
CREATE INDEX idx_notifications_recipient_read ON notifications(recipient_id, read_at);

CREATE TABLE notification_deliveries (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    provider VARCHAR(80),
    provider_message_id VARCHAR(255),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE,
    sent_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    failure_code VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_notification_delivery_attempts CHECK (attempt_count >= 0)
);
CREATE INDEX idx_notification_deliveries_status_retry ON notification_deliveries(status, next_attempt_at);

CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(80) NOT NULL,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_notification_preferences_user_type UNIQUE (user_id, type)
);

CREATE TABLE user_devices (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    platform VARCHAR(20) NOT NULL,
    push_token VARCHAR(1000) NOT NULL,
    device_name VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_devices_push_token UNIQUE (push_token)
);
CREATE INDEX idx_user_devices_user_active ON user_devices(user_id, active);

CREATE TABLE system_user_roles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_system_user_roles_user_role UNIQUE (user_id, role)
);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    actor_id UUID REFERENCES users(id),
    actor_type VARCHAR(40) NOT NULL,
    workspace_id UUID REFERENCES workspaces(id),
    project_id UUID REFERENCES research_projects(id) ON DELETE SET NULL,
    type VARCHAR(80) NOT NULL,
    target_type VARCHAR(120),
    target_id UUID,
    ip_address_hash VARCHAR(128),
    user_agent_summary VARCHAR(255),
    metadata_json TEXT,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_audit_events_occurred ON audit_events(occurred_at);
CREATE INDEX idx_audit_events_actor_time ON audit_events(actor_id, occurred_at);
CREATE INDEX idx_audit_events_workspace ON audit_events(workspace_id, occurred_at);
CREATE INDEX idx_audit_events_type ON audit_events(type);

INSERT INTO subscription_plans (id, code, name, description, status, billing_interval, price, currency, publicly_available, display_order)
VALUES
('00000000-0000-0000-0000-000000000101','FREE','Free','Development free plan with small quotas.','ACTIVE','NONE',0,'GHS',TRUE,1),
('00000000-0000-0000-0000-000000000102','STUDENT','Student','Development student plan.','ACTIVE','MONTHLY',50,'GHS',TRUE,2),
('00000000-0000-0000-0000-000000000103','PRO','Pro','Development professional plan.','ACTIVE','MONTHLY',150,'GHS',TRUE,3),
('00000000-0000-0000-0000-000000000104','INSTITUTION','Institution','Development institution plan.','ACTIVE','MONTHLY',500,'GHS',FALSE,4);

INSERT INTO plan_entitlements (id, plan_id, feature, enabled, limit_value, limit_unit)
VALUES
('00000000-0000-0000-0000-000000001001','00000000-0000-0000-0000-000000000101','AI_GENERATION',TRUE,100,'REQUESTS'),
('00000000-0000-0000-0000-000000001002','00000000-0000-0000-0000-000000000101','AI_TOKENS',TRUE,500000,'TOKENS'),
('00000000-0000-0000-0000-000000001003','00000000-0000-0000-0000-000000000101','PROJECT_CREATION',TRUE,5,'PROJECTS'),
('00000000-0000-0000-0000-000000001004','00000000-0000-0000-0000-000000000101','COLLABORATORS_PER_PROJECT',TRUE,5,'USERS'),
('00000000-0000-0000-0000-000000001005','00000000-0000-0000-0000-000000000101','DOCUMENT_UPLOAD',TRUE,25,'DOCUMENTS'),
('00000000-0000-0000-0000-000000001006','00000000-0000-0000-0000-000000000101','STORAGE',TRUE,5368709120,'BYTES'),
('00000000-0000-0000-0000-000000001007','00000000-0000-0000-0000-000000000101','REPORT_EXPORT_DOCX',TRUE,10,'EXPORTS'),
('00000000-0000-0000-0000-000000001008','00000000-0000-0000-0000-000000000101','REPORT_EXPORT_PDF',FALSE,0,'EXPORTS'),
('00000000-0000-0000-0000-000000001009','00000000-0000-0000-0000-000000000102','AI_GENERATION',TRUE,1000,'REQUESTS'),
('00000000-0000-0000-0000-000000001010','00000000-0000-0000-0000-000000000102','AI_TOKENS',TRUE,3000000,'TOKENS'),
('00000000-0000-0000-0000-000000001011','00000000-0000-0000-0000-000000000102','PROJECT_CREATION',TRUE,20,'PROJECTS'),
('00000000-0000-0000-0000-000000001012','00000000-0000-0000-0000-000000000102','COLLABORATORS_PER_PROJECT',TRUE,10,'USERS'),
('00000000-0000-0000-0000-000000001013','00000000-0000-0000-0000-000000000102','STORAGE',TRUE,21474836480,'BYTES'),
('00000000-0000-0000-0000-000000001014','00000000-0000-0000-0000-000000000102','REPORT_EXPORT_DOCX',TRUE,100,'EXPORTS'),
('00000000-0000-0000-0000-000000001015','00000000-0000-0000-0000-000000000102','REPORT_EXPORT_PDF',TRUE,100,'EXPORTS');
