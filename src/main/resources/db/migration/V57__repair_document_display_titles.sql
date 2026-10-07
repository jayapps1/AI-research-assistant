-- Repair legacy or manually migrated documents that lack a logical
-- display title, then enforce nonblank titles at the database boundary.

UPDATE documents d
SET title = LEFT(
        COALESCE(
                NULLIF(BTRIM(d.bibliographic_title), ''),
                NULLIF(
                        BTRIM(
                                REGEXP_REPLACE(
                                        COALESCE(dv.original_filename, ''),
                                        '\.[^.]*$',
                                        ''
                                )
                        ),
                        ''
                ),
                d.document_code
        ),
        500
    )
FROM document_versions dv
WHERE dv.id = d.current_version_id
  AND (d.title IS NULL OR BTRIM(d.title) = '');

UPDATE documents d
SET title = LEFT(
        COALESCE(
                NULLIF(BTRIM(d.bibliographic_title), ''),
                d.document_code
        ),
        500
    )
WHERE d.title IS NULL OR BTRIM(d.title) = '';

ALTER TABLE documents
    ALTER COLUMN title SET NOT NULL;

ALTER TABLE documents
    DROP CONSTRAINT IF EXISTS chk_documents_title_nonblank;

ALTER TABLE documents
    ADD CONSTRAINT chk_documents_title_nonblank
        CHECK (BTRIM(title) <> '');
