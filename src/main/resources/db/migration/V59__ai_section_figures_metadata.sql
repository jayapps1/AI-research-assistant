-- V59: Structured AI-generated section figures

ALTER TABLE project_evidence
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(80),
    ADD COLUMN IF NOT EXISTS generation_source VARCHAR(120),
    ADD COLUMN IF NOT EXISTS structured_definition TEXT,
    ADD COLUMN IF NOT EXISTS evidence_source_ids TEXT;

CREATE INDEX IF NOT EXISTS idx_project_evidence_source_type ON project_evidence(source_type);
