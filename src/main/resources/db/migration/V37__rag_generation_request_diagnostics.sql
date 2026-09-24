ALTER TABLE rag_queries
    ADD COLUMN IF NOT EXISTS selected_document_count INTEGER,
    ADD COLUMN IF NOT EXISTS candidate_chunk_count INTEGER,
    ADD COLUMN IF NOT EXISTS selected_evidence_count INTEGER,
    ADD COLUMN IF NOT EXISTS estimated_input_tokens INTEGER,
    ADD COLUMN IF NOT EXISTS actual_input_tokens INTEGER,
    ADD COLUMN IF NOT EXISTS actual_output_tokens INTEGER,
    ADD COLUMN IF NOT EXISTS context_budget_tokens INTEGER,
    ADD COLUMN IF NOT EXISTS truncated_evidence_count INTEGER,
    ADD COLUMN IF NOT EXISTS generation_strategy VARCHAR(60),
    ADD COLUMN IF NOT EXISTS configured_model VARCHAR(120);
