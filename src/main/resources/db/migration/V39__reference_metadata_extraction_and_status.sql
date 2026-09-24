-- ============================================================
-- AI RESEARCH ASSISTANT
-- V39 - REFERENCE METADATA EXTRACTION AND STATUS
-- ============================================================

ALTER TABLE document_processing_jobs
    DROP CONSTRAINT IF EXISTS chk_document_processing_jobs_type;

ALTER TABLE document_processing_jobs
    ADD CONSTRAINT chk_document_processing_jobs_type
        CHECK (
            type IN (
                'INGESTION',
                'TEXT_EXTRACTION',
                'REFERENCE_METADATA_EXTRACTION',
                'CHUNKING',
                'EMBEDDING',
                'INDEXING'
            )
        );

ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS bibliographic_metadata_status VARCHAR(40) NOT NULL DEFAULT 'INCOMPLETE',
    ADD COLUMN IF NOT EXISTS bibliographic_metadata_source VARCHAR(80),
    ADD COLUMN IF NOT EXISTS bibliographic_metadata_confidence DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS bibliographic_metadata_extracted_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE documents
    DROP CONSTRAINT IF EXISTS chk_documents_bibliographic_metadata_status;

ALTER TABLE documents
    ADD CONSTRAINT chk_documents_bibliographic_metadata_status
        CHECK (
            bibliographic_metadata_status IN (
                'COMPLETE',
                'PARTIAL',
                'INCOMPLETE',
                'NEEDS_REVIEW',
                'VERIFIED'
            )
        );

ALTER TABLE reference_entries
    DROP CONSTRAINT IF EXISTS reference_entries_metadata_status_check;

ALTER TABLE reference_entries
    DROP CONSTRAINT IF EXISTS chk_reference_entries_metadata_status;

ALTER TABLE reference_entries
    ADD CONSTRAINT chk_reference_entries_metadata_status
        CHECK (
            metadata_status IN (
                'COMPLETE',
                'PARTIAL',
                'INCOMPLETE',
                'NEEDS_REVIEW',
                'VERIFIED'
            )
        );

ALTER TABLE reference_entries
    ADD COLUMN IF NOT EXISTS metadata_source VARCHAR(80),
    ADD COLUMN IF NOT EXISTS metadata_confidence DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS metadata_review_status VARCHAR(40),
    ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP WITH TIME ZONE;

