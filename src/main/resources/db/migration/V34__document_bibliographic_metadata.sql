-- ============================================================
-- AI RESEARCH ASSISTANT
-- V34 - DOCUMENT BIBLIOGRAPHIC METADATA
-- ============================================================
-- Keeps document identity/storage/processing metadata separate from
-- research-source bibliographic fields used by references and citations.

ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS bibliographic_title VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS authors TEXT,
    ADD COLUMN IF NOT EXISTS publication_year INTEGER,
    ADD COLUMN IF NOT EXISTS journal VARCHAR(500),
    ADD COLUMN IF NOT EXISTS conference VARCHAR(500),
    ADD COLUMN IF NOT EXISTS publisher VARCHAR(500),
    ADD COLUMN IF NOT EXISTS volume VARCHAR(100),
    ADD COLUMN IF NOT EXISTS issue VARCHAR(100),
    ADD COLUMN IF NOT EXISTS pages VARCHAR(100),
    ADD COLUMN IF NOT EXISTS doi VARCHAR(500),
    ADD COLUMN IF NOT EXISTS url VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(80),
    ADD COLUMN IF NOT EXISTS keywords TEXT;

CREATE INDEX IF NOT EXISTS idx_documents_publication_year ON documents(publication_year);
CREATE INDEX IF NOT EXISTS idx_documents_doi ON documents(doi);
