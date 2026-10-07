-- V56: Add document chunk semantic type for retrieval hygiene and RAG evidence filtering

-- 1. Add semantic_type column to document_chunks table (nullable initially for safe backfill)
ALTER TABLE document_chunks
    ADD COLUMN IF NOT EXISTS semantic_type VARCHAR(50);

-- 2. Classify and backfill existing document_chunks based on content hygiene heuristics
UPDATE document_chunks
SET semantic_type = CASE
    -- References / Bibliography
    WHEN text_content ~* '^\s*(#{1,4}\s*)?(references|bibliography|works\s+cited|literature\s+cited)\s*(\r?\n|$)' THEN 'REFERENCES'
    -- Table captions
    WHEN text_content ~* '^\s*(table|tbl\.)\s+[0-9]+[:.]' THEN 'TABLE'
    -- Figure captions
    WHEN text_content ~* '^\s*(figure|fig\.)\s+[0-9]+[:.]' THEN 'FIGURE_CAPTION'
    -- Footers / Running headers
    WHEN length(trim(text_content)) <= 300 AND text_content ~* '(available\s+online\s+at|journal\s+homepage|contents\s+lists\s+available\s+at|all\s+rights\s+reserved|elsevier|springer|wiley|ieee|page\s+[0-9]+\s+of\s+[0-9]+)' THEN 'FOOTER_HEADER'
    -- Metadata snippets
    WHEN length(trim(text_content)) <= 400 AND text_content ~* '(received\s+[0-9]{1,2}\s+[a-z]+\s+[0-9]{4}|article\s+history|corresponding\s+author|\bkeywords\s*:)' THEN 'METADATA'
    -- Front matter
    WHEN chunk_number <= 2 AND (text_content ~* '(received\s+[0-9]{1,2}\s+[a-z]+\s+[0-9]{4}|article\s+history)' OR (text_content ILIKE '%@%' AND text_content ILIKE '%University%')) THEN 'FRONT_MATTER'
    -- Substantive Body content
    ELSE 'BODY'
END
WHERE semantic_type IS NULL;

-- 3. Ensure no records remain NULL (safe fallback to BODY)
UPDATE document_chunks
SET semantic_type = 'BODY'
WHERE semantic_type IS NULL;

-- 4. Enforce NOT NULL constraint now that all rows are populated
ALTER TABLE document_chunks
    ALTER COLUMN semantic_type SET NOT NULL;

-- 5. Add CHECK constraint matching exact ChunkSemanticType enum values
ALTER TABLE document_chunks
    DROP CONSTRAINT IF EXISTS chk_document_chunks_semantic_type;

ALTER TABLE document_chunks
    ADD CONSTRAINT chk_document_chunks_semantic_type
        CHECK (semantic_type IN (
            'FRONT_MATTER',
            'BODY',
            'TABLE',
            'FIGURE_CAPTION',
            'REFERENCES',
            'FOOTER_HEADER',
            'METADATA'
        ));

-- 6. Indexes for chunk semantic filtering
CREATE INDEX IF NOT EXISTS idx_document_chunks_semantic_type
    ON document_chunks(semantic_type);

CREATE INDEX IF NOT EXISTS idx_document_chunks_version_semantic
    ON document_chunks(document_version_id, semantic_type);
