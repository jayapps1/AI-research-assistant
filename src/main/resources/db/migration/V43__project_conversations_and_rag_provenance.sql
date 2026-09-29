-- ============================================================
-- AI RESEARCH ASSISTANT
-- V43 - PROJECT CONVERSATIONS AND RAG PROVENANCE
-- ============================================================

ALTER TABLE conversations
    DROP CONSTRAINT IF EXISTS chk_conversations_type;

ALTER TABLE conversations
    ADD CONSTRAINT chk_conversations_type
        CHECK (type IN ('GENERAL', 'PROJECT'));

UPDATE conversations
SET type = 'PROJECT'
WHERE project_id IS NOT NULL
  AND type <> 'PROJECT';

ALTER TABLE conversation_runs
    ADD COLUMN IF NOT EXISTS rag_query_id UUID;

ALTER TABLE conversation_runs
    DROP CONSTRAINT IF EXISTS fk_conversation_runs_rag_query;

ALTER TABLE conversation_runs
    ADD CONSTRAINT fk_conversation_runs_rag_query
        FOREIGN KEY (rag_query_id) REFERENCES rag_queries(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_conversation_runs_rag_query
    ON conversation_runs(rag_query_id);

CREATE INDEX IF NOT EXISTS idx_conversations_project_user_status_last_message
    ON conversations(project_id, user_id, status, last_message_at DESC NULLS LAST);
