-- ============================================================
-- AI RESEARCH ASSISTANT
-- V9 - DOCUMENT EXTRACTION, PAGES, CHUNKS AND EMBEDDINGS
-- ============================================================
--
-- This migration adds citation-preserving derived document data.
-- It intentionally does not require pgvector because the current
-- local database does not expose the vector extension and embeddings
-- are disabled by default. document_chunk_embeddings.vector_values is
-- model-aware metadata storage for disabled/test providers; production
-- pgvector indexing should be added in a separate deployment migration
-- once the extension is installed and the selected model dimensions
-- are known.
-- ============================================================

ALTER TABLE document_processing_jobs
    DROP CONSTRAINT chk_document_processing_jobs_type;

ALTER TABLE document_processing_jobs
    ADD CONSTRAINT chk_document_processing_jobs_type
        CHECK (
            type IN (
                'INGESTION',
                'TEXT_EXTRACTION',
                'CHUNKING',
                'EMBEDDING',
                'INDEXING'
            )
        );

CREATE TABLE document_text_extractions
(
    id UUID PRIMARY KEY,
    document_version_id UUID NOT NULL,
    status VARCHAR(40) NOT NULL,
    extractor VARCHAR(100) NOT NULL,
    extractor_version VARCHAR(100),
    page_count INTEGER NOT NULL,
    character_count BIGINT NOT NULL,
    language VARCHAR(50),
    content_checksum_sha256 VARCHAR(64),
    ocr_required BOOLEAN NOT NULL DEFAULT FALSE,
    failure_code VARCHAR(100),
    failure_message VARCHAR(2000),
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_document_text_extractions_version
        FOREIGN KEY (document_version_id)
        REFERENCES document_versions(id),

    CONSTRAINT uk_document_text_extractions_version
        UNIQUE (document_version_id),

    CONSTRAINT chk_document_text_extractions_status
        CHECK (
            status IN (
                'PENDING',
                'RUNNING',
                'COMPLETED',
                'FAILED',
                'OCR_REQUIRED'
            )
        ),

    CONSTRAINT chk_document_text_extractions_page_count
        CHECK (page_count >= 0),

    CONSTRAINT chk_document_text_extractions_character_count
        CHECK (character_count >= 0)
);

CREATE INDEX idx_document_text_extractions_version_id
    ON document_text_extractions(document_version_id);

CREATE INDEX idx_document_text_extractions_status
    ON document_text_extractions(status);

CREATE TABLE document_pages
(
    id UUID PRIMARY KEY,
    document_version_id UUID NOT NULL,
    page_number INTEGER NOT NULL,
    source_label VARCHAR(100),
    text_content TEXT NOT NULL,
    character_count BIGINT NOT NULL,
    content_checksum_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_document_pages_version
        FOREIGN KEY (document_version_id)
        REFERENCES document_versions(id),

    CONSTRAINT chk_document_pages_page_number
        CHECK (page_number >= 1),

    CONSTRAINT chk_document_pages_character_count
        CHECK (character_count >= 0),

    CONSTRAINT uk_document_pages_version_page
        UNIQUE (document_version_id, page_number)
);

CREATE INDEX idx_document_pages_version_id
    ON document_pages(document_version_id);

CREATE INDEX idx_document_pages_version_page
    ON document_pages(document_version_id, page_number);

CREATE TABLE document_chunks
(
    id UUID PRIMARY KEY,
    document_version_id UUID NOT NULL,
    page_id UUID NOT NULL,
    chunk_number INTEGER NOT NULL,
    text_content TEXT NOT NULL,
    character_start INTEGER NOT NULL,
    character_end INTEGER NOT NULL,
    character_count BIGINT NOT NULL,
    estimated_token_count INTEGER,
    content_checksum_sha256 VARCHAR(64) NOT NULL,
    heading VARCHAR(1000),
    search_vector tsvector GENERATED ALWAYS AS
        (to_tsvector('english', coalesce(text_content, ''))) STORED,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_document_chunks_version
        FOREIGN KEY (document_version_id)
        REFERENCES document_versions(id),

    CONSTRAINT fk_document_chunks_page
        FOREIGN KEY (page_id)
        REFERENCES document_pages(id),

    CONSTRAINT chk_document_chunks_start
        CHECK (character_start >= 0),

    CONSTRAINT chk_document_chunks_end
        CHECK (character_end > character_start),

    CONSTRAINT chk_document_chunks_character_count
        CHECK (character_count >= 0),

    CONSTRAINT uk_document_chunks_version_number
        UNIQUE (document_version_id, chunk_number)
);

CREATE INDEX idx_document_chunks_version_id
    ON document_chunks(document_version_id);

CREATE INDEX idx_document_chunks_page_id
    ON document_chunks(page_id);

CREATE INDEX idx_document_chunks_version_number
    ON document_chunks(document_version_id, chunk_number);

CREATE INDEX idx_document_chunks_search_vector
    ON document_chunks
    USING GIN(search_vector);

CREATE TABLE document_chunk_embeddings
(
    id UUID PRIMARY KEY,
    chunk_id UUID NOT NULL,
    provider VARCHAR(100) NOT NULL,
    model VARCHAR(200) NOT NULL,
    dimensions INTEGER NOT NULL,
    chunk_checksum_sha256 VARCHAR(64) NOT NULL,
    status VARCHAR(40) NOT NULL,
    vector_values DOUBLE PRECISION[],
    failure_code VARCHAR(100),
    failure_message VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    embedded_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_document_chunk_embeddings_chunk
        FOREIGN KEY (chunk_id)
        REFERENCES document_chunks(id),

    CONSTRAINT uk_document_chunk_embeddings_chunk_provider_model
        UNIQUE (chunk_id, provider, model),

    CONSTRAINT chk_document_chunk_embeddings_dimensions
        CHECK (dimensions > 0),

    CONSTRAINT chk_document_chunk_embeddings_status
        CHECK (
            status IN (
                'PENDING',
                'RUNNING',
                'COMPLETED',
                'FAILED',
                'STALE',
                'SKIPPED'
            )
        )
);

CREATE INDEX idx_document_chunk_embeddings_chunk_id
    ON document_chunk_embeddings(chunk_id);

CREATE INDEX idx_document_chunk_embeddings_provider_model_status
    ON document_chunk_embeddings(provider, model, status);
