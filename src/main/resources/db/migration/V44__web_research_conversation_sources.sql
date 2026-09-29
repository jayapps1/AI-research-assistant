-- ============================================================
-- AI RESEARCH ASSISTANT
-- V44 - WEB RESEARCH, CONVERSATION SOURCES, AND MESSAGE CITATIONS
-- ============================================================

ALTER TABLE conversation_runs
    ADD COLUMN IF NOT EXISTS total_tokens INTEGER,
    ADD COLUMN IF NOT EXISTS cached_input_tokens INTEGER,
    ADD COLUMN IF NOT EXISTS failure_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS web_provider VARCHAR(80),
    ADD COLUMN IF NOT EXISTS web_result_count INTEGER,
    ADD COLUMN IF NOT EXISTS web_provider_cost NUMERIC(12, 6);

ALTER TABLE conversation_runs
    DROP CONSTRAINT IF EXISTS chk_conversation_runs_web_result_count;

ALTER TABLE conversation_runs
    ADD CONSTRAINT chk_conversation_runs_web_result_count
        CHECK (web_result_count IS NULL OR web_result_count >= 0);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'conversation_runs'
          AND column_name = 'error_code'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'conversation_runs'
          AND column_name = 'failure_code'
    ) THEN
        ALTER TABLE conversation_runs RENAME COLUMN error_code TO failure_code;
    ELSIF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'conversation_runs'
          AND column_name = 'error_code'
    ) THEN
        UPDATE conversation_runs
        SET failure_code = COALESCE(failure_code, error_code);
    END IF;
END $$;

ALTER TABLE conversation_messages
    ALTER COLUMN sequence_number TYPE INTEGER USING sequence_number::INTEGER;

ALTER TABLE reference_entries
    ADD COLUMN IF NOT EXISTS accessed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS retrieved_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE IF NOT EXISTS conversation_sources
(
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    message_id UUID NOT NULL,
    source_type VARCHAR(60) NOT NULL,
    document_id UUID,
    document_version_id UUID,
    project_reference_id UUID,
    title TEXT NOT NULL,
    url TEXT,
    provider VARCHAR(80),
    authors_json TEXT,
    published_at TIMESTAMP WITH TIME ZONE,
    retrieved_at TIMESTAMP WITH TIME ZONE NOT NULL,
    source_ordinal INTEGER,
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_conversation_sources_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,

    CONSTRAINT fk_conversation_sources_message
        FOREIGN KEY (message_id) REFERENCES conversation_messages(id) ON DELETE CASCADE,

    CONSTRAINT fk_conversation_sources_document
        FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE SET NULL,

    CONSTRAINT fk_conversation_sources_document_version
        FOREIGN KEY (document_version_id) REFERENCES document_versions(id) ON DELETE SET NULL,

    CONSTRAINT fk_conversation_sources_project_reference
        FOREIGN KEY (project_reference_id) REFERENCES project_references(id) ON DELETE SET NULL,

    CONSTRAINT chk_conversation_sources_type
        CHECK (source_type IN ('PROJECT_DOCUMENT', 'PROJECT_REFERENCE', 'WEB_PAGE', 'ACADEMIC_SOURCE', 'PROJECT_ANALYSIS')),

    CONSTRAINT chk_conversation_sources_ordinal
        CHECK (source_ordinal IS NULL OR source_ordinal >= 1)
);

ALTER TABLE conversation_sources
    ADD COLUMN IF NOT EXISTS authors_json TEXT,
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS retrieved_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS source_ordinal INTEGER;

UPDATE conversation_sources
SET retrieved_at = COALESCE(retrieved_at, created_at, CURRENT_TIMESTAMP)
WHERE retrieved_at IS NULL;

ALTER TABLE conversation_sources
    DROP CONSTRAINT IF EXISTS fk_conversation_sources_message,
    DROP CONSTRAINT IF EXISTS chk_conversation_sources_type,
    DROP CONSTRAINT IF EXISTS chk_conversation_sources_ordinal;

ALTER TABLE conversation_sources
    ALTER COLUMN title TYPE TEXT USING title::TEXT,
    ALTER COLUMN url TYPE TEXT USING url::TEXT,
    ALTER COLUMN message_id SET NOT NULL,
    ALTER COLUMN retrieved_at SET NOT NULL;

ALTER TABLE conversation_sources
    ADD CONSTRAINT fk_conversation_sources_message
        FOREIGN KEY (message_id) REFERENCES conversation_messages(id) ON DELETE CASCADE,
    ADD CONSTRAINT chk_conversation_sources_type
        CHECK (source_type IN ('PROJECT_DOCUMENT', 'PROJECT_REFERENCE', 'WEB_PAGE', 'ACADEMIC_SOURCE', 'PROJECT_ANALYSIS')),
    ADD CONSTRAINT chk_conversation_sources_ordinal
        CHECK (source_ordinal IS NULL OR source_ordinal >= 1);

CREATE INDEX IF NOT EXISTS idx_conversation_sources_conversation
    ON conversation_sources(conversation_id);

CREATE INDEX IF NOT EXISTS idx_conversation_sources_message
    ON conversation_sources(message_id);

CREATE INDEX IF NOT EXISTS idx_conversation_sources_url
    ON conversation_sources(url);


CREATE TABLE IF NOT EXISTS message_citations
(
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL,
    conversation_source_id UUID NOT NULL,
    citation_ordinal INTEGER NOT NULL,
    marker VARCHAR(40),
    claim_text TEXT,
    supporting_excerpt TEXT,
    formatted_citation TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_message_citations_message
        FOREIGN KEY (message_id) REFERENCES conversation_messages(id) ON DELETE CASCADE,

    CONSTRAINT fk_message_citations_source
        FOREIGN KEY (conversation_source_id) REFERENCES conversation_sources(id) ON DELETE CASCADE,

    CONSTRAINT chk_message_citations_ordinal
        CHECK (citation_ordinal >= 1),

    CONSTRAINT uk_message_citations_ordinal
        UNIQUE (message_id, citation_ordinal)
);

ALTER TABLE message_citations
    DROP CONSTRAINT IF EXISTS fk_message_citations_source,
    DROP CONSTRAINT IF EXISTS chk_message_citations_ordinal,
    DROP CONSTRAINT IF EXISTS uk_message_citations_ordinal;

DROP INDEX IF EXISTS idx_message_citations_source;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'message_citations'
          AND column_name = 'source_id'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'message_citations'
          AND column_name = 'conversation_source_id'
    ) THEN
        ALTER TABLE message_citations RENAME COLUMN source_id TO conversation_source_id;
    ELSIF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'message_citations'
          AND column_name = 'source_id'
    ) AND EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'message_citations'
          AND column_name = 'conversation_source_id'
    ) THEN
        UPDATE message_citations
        SET conversation_source_id = COALESCE(conversation_source_id, source_id);
        ALTER TABLE message_citations DROP COLUMN source_id;
    END IF;
END $$;

ALTER TABLE message_citations
    ADD COLUMN IF NOT EXISTS conversation_source_id UUID,
    ADD COLUMN IF NOT EXISTS marker VARCHAR(40);

ALTER TABLE message_citations
    ALTER COLUMN conversation_source_id SET NOT NULL;

ALTER TABLE message_citations
    ADD CONSTRAINT fk_message_citations_source
        FOREIGN KEY (conversation_source_id) REFERENCES conversation_sources(id) ON DELETE CASCADE,
    ADD CONSTRAINT chk_message_citations_ordinal
        CHECK (citation_ordinal >= 1),
    ADD CONSTRAINT uk_message_citations_ordinal
        UNIQUE (message_id, citation_ordinal);

CREATE INDEX IF NOT EXISTS idx_message_citations_message
    ON message_citations(message_id);

CREATE INDEX IF NOT EXISTS idx_message_citations_source
    ON message_citations(conversation_source_id);
