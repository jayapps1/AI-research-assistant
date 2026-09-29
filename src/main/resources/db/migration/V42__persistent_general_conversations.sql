-- ============================================================
-- AI RESEARCH ASSISTANT
-- V42 - PERSISTENT GENERAL CONVERSATIONS
-- ============================================================

CREATE TABLE IF NOT EXISTS conversations
(
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    workspace_id UUID,
    project_id UUID,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_message_at TIMESTAMP WITH TIME ZONE,
    archived_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_conversations_user
        FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT fk_conversations_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE SET NULL,

    CONSTRAINT fk_conversations_project
        FOREIGN KEY (project_id) REFERENCES research_projects(id) ON DELETE SET NULL,

    CONSTRAINT chk_conversations_type
        CHECK (type IN ('GENERAL')),

    CONSTRAINT chk_conversations_status
        CHECK (status IN ('ACTIVE', 'ARCHIVED', 'TRASHED'))
);

CREATE INDEX IF NOT EXISTS idx_conversations_user_last_message
    ON conversations(user_id, last_message_at DESC NULLS LAST);

CREATE INDEX IF NOT EXISTS idx_conversations_user_status_last_message
    ON conversations(user_id, status, last_message_at DESC NULLS LAST);

CREATE INDEX IF NOT EXISTS idx_conversations_workspace_last_message
    ON conversations(workspace_id, last_message_at DESC NULLS LAST);

CREATE INDEX IF NOT EXISTS idx_conversations_project_last_message
    ON conversations(project_id, last_message_at DESC NULLS LAST);


CREATE TABLE IF NOT EXISTS conversation_messages
(
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    role VARCHAR(30) NOT NULL,
    content TEXT NOT NULL,
    structured_content TEXT,
    sequence_number INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_conversation_messages_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,

    CONSTRAINT chk_conversation_messages_role
        CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM')),

    CONSTRAINT chk_conversation_messages_sequence
        CHECK (sequence_number > 0),

    CONSTRAINT uk_conversation_messages_sequence
        UNIQUE (conversation_id, sequence_number)
);

CREATE INDEX IF NOT EXISTS idx_conversation_messages_conversation_sequence
    ON conversation_messages(conversation_id, sequence_number);

CREATE INDEX IF NOT EXISTS idx_conversation_messages_conversation_role
    ON conversation_messages(conversation_id, role);


CREATE TABLE IF NOT EXISTS conversation_runs
(
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    user_message_id UUID NOT NULL,
    assistant_message_id UUID,
    ai_request_id UUID,
    operation_type VARCHAR(60) NOT NULL,
    search_scope VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    provider VARCHAR(40),
    model VARCHAR(200),
    input_tokens INTEGER,
    output_tokens INTEGER,
    total_tokens INTEGER,
    cached_input_tokens INTEGER,
    provider_cost NUMERIC(12, 6),
    platform_credits NUMERIC(14, 4),
    failure_code VARCHAR(100),
    failure_message VARCHAR(1000),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_conversation_runs_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,

    CONSTRAINT fk_conversation_runs_user_message
        FOREIGN KEY (user_message_id) REFERENCES conversation_messages(id) ON DELETE CASCADE,

    CONSTRAINT fk_conversation_runs_assistant_message
        FOREIGN KEY (assistant_message_id) REFERENCES conversation_messages(id) ON DELETE SET NULL,

    CONSTRAINT fk_conversation_runs_ai_request
        FOREIGN KEY (ai_request_id) REFERENCES ai_requests(id) ON DELETE SET NULL,

    CONSTRAINT chk_conversation_runs_status
        CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')),

    CONSTRAINT chk_conversation_runs_tokens
        CHECK (
            (input_tokens IS NULL OR input_tokens >= 0) AND
            (output_tokens IS NULL OR output_tokens >= 0) AND
            (total_tokens IS NULL OR total_tokens >= 0) AND
            (cached_input_tokens IS NULL OR cached_input_tokens >= 0)
        )
);

CREATE INDEX IF NOT EXISTS idx_conversation_runs_conversation_created
    ON conversation_runs(conversation_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_conversation_runs_user_message
    ON conversation_runs(user_message_id);

CREATE INDEX IF NOT EXISTS idx_conversation_runs_ai_request
    ON conversation_runs(ai_request_id);
