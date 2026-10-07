-- V58: Persist staged institution template/guideline uploads independently of project creation.

ALTER TABLE academic_document_guidelines
    ADD COLUMN IF NOT EXISTS storage_object_id UUID REFERENCES storage_objects(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_guidelines_storage_object_id ON academic_document_guidelines(storage_object_id);
