-- ============================================================
-- AI RESEARCH ASSISTANT
-- V18 - AI REQUESTS, USAGE COSTS AND MODEL PRICING
-- ============================================================

CREATE TABLE ai_requests
(
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    workspace_id UUID NOT NULL,
    project_id UUID,
    task_type VARCHAR(60) NOT NULL,
    provider VARCHAR(40) NOT NULL,
    model VARCHAR(200),
    status VARCHAR(40) NOT NULL,
    idempotency_key VARCHAR(128),
    input_tokens INTEGER,
    output_tokens INTEGER,
    total_tokens INTEGER,
    latency_ms BIGINT,
    provider_request_id VARCHAR(255),
    failure_category VARCHAR(60),
    failure_code VARCHAR(100),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ai_requests_user
        FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT fk_ai_requests_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces(id),

    CONSTRAINT fk_ai_requests_project
        FOREIGN KEY (project_id) REFERENCES research_projects(id) ON DELETE SET NULL,

    CONSTRAINT chk_ai_requests_tokens
        CHECK (
            (input_tokens IS NULL OR input_tokens >= 0) AND
            (output_tokens IS NULL OR output_tokens >= 0) AND
            (total_tokens IS NULL OR total_tokens >= 0)
        ),

    CONSTRAINT chk_ai_requests_latency
        CHECK (latency_ms IS NULL OR latency_ms >= 0),

    CONSTRAINT chk_ai_requests_status
        CHECK (
            status IN (
                'RECEIVED',
                'RUNNING',
                'COMPLETED',
                'FAILED',
                'TIMED_OUT',
                'REJECTED_BY_POLICY',
                'CAPABILITY_UNAVAILABLE',
                'CANCELLED'
            )
        )
);

CREATE INDEX idx_ai_requests_user_created
    ON ai_requests(user_id, created_at);

CREATE INDEX idx_ai_requests_workspace_created
    ON ai_requests(workspace_id, created_at);

CREATE INDEX idx_ai_requests_project_created
    ON ai_requests(project_id, created_at);

CREATE INDEX idx_ai_requests_task_created
    ON ai_requests(task_type, created_at);

CREATE INDEX idx_ai_requests_status_created
    ON ai_requests(status, created_at);

CREATE INDEX idx_ai_requests_idempotency
    ON ai_requests(idempotency_key);


CREATE TABLE ai_model_pricing
(
    id UUID PRIMARY KEY,
    provider VARCHAR(40) NOT NULL,
    model VARCHAR(200) NOT NULL,
    input_price_per_million NUMERIC(12, 6),
    output_price_per_million NUMERIC(12, 6),
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_ai_model_pricing_input_price
        CHECK (input_price_per_million IS NULL OR input_price_per_million >= 0),

    CONSTRAINT chk_ai_model_pricing_output_price
        CHECK (output_price_per_million IS NULL OR output_price_per_million >= 0)
);

CREATE INDEX idx_ai_model_pricing_provider_model
    ON ai_model_pricing(provider, model, active);


CREATE TABLE ai_usage_costs
(
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    input_cost NUMERIC(12, 6),
    output_cost NUMERIC(12, 6),
    total_cost NUMERIC(12, 6),
    source VARCHAR(40) NOT NULL,
    pricing_version VARCHAR(100),
    calculated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ai_usage_costs_request
        FOREIGN KEY (request_id) REFERENCES ai_requests(id) ON DELETE CASCADE,

    CONSTRAINT chk_ai_usage_costs_costs
        CHECK (
            (input_cost IS NULL OR input_cost >= 0) AND
            (output_cost IS NULL OR output_cost >= 0) AND
            (total_cost IS NULL OR total_cost >= 0)
        ),

    CONSTRAINT chk_ai_usage_costs_source
        CHECK (
            source IN (
                'PROVIDER_REPORTED',
                'CONFIGURED_PRICING',
                'ESTIMATED',
                'UNAVAILABLE'
            )
        )
);

CREATE INDEX idx_ai_usage_costs_request_id
    ON ai_usage_costs(request_id);
