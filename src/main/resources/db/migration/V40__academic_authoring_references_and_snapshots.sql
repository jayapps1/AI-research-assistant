-- ============================================================
-- AI RESEARCH ASSISTANT
-- V40 - ACADEMIC AUTHORING, CITATION FLAGS, AND FINAL SNAPSHOTS
-- ============================================================

ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS isbn VARCHAR(80),
    ADD COLUMN IF NOT EXISTS issn VARCHAR(80);

ALTER TABLE project_references
    ADD COLUMN IF NOT EXISTS available_for_research_ai BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS available_for_citation BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS include_when_cited_in_bibliography BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE research_report_sections
    ADD COLUMN IF NOT EXISTS content_json TEXT,
    ADD COLUMN IF NOT EXISTS plain_text TEXT,
    ADD COLUMN IF NOT EXISTS status VARCHAR(40) NOT NULL DEFAULT 'NOT_STARTED';

ALTER TABLE research_report_sections
    DROP CONSTRAINT IF EXISTS chk_research_report_sections_status;

ALTER TABLE research_report_sections
    ADD CONSTRAINT chk_research_report_sections_status
        CHECK (status IN ('NOT_STARTED','DRAFT','IN_REVIEW','ACCEPTED'));

UPDATE research_report_sections
SET plain_text = COALESCE(plain_text, content),
    status = CASE
        WHEN status IS NOT NULL AND status <> 'NOT_STARTED' THEN status
        WHEN content IS NULL OR btrim(content) = '' THEN 'NOT_STARTED'
        ELSE 'DRAFT'
    END;

CREATE TABLE IF NOT EXISTS research_report_section_versions (
    id UUID PRIMARY KEY,
    section_id UUID NOT NULL REFERENCES research_report_sections(id) ON DELETE CASCADE,
    revision_number INTEGER NOT NULL CHECK (revision_number >= 1),
    label VARCHAR(120),
    content TEXT,
    content_json TEXT,
    plain_text TEXT,
    status VARCHAR(40) NOT NULL CHECK (status IN ('NOT_STARTED','DRAFT','IN_REVIEW','ACCEPTED')),
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(section_id, revision_number)
);

CREATE INDEX IF NOT EXISTS idx_report_section_versions_section
    ON research_report_section_versions(section_id);

CREATE TABLE IF NOT EXISTS report_document_versions (
    id UUID PRIMARY KEY,
    report_id UUID NOT NULL REFERENCES research_reports(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    version_number INTEGER NOT NULL CHECK (version_number >= 1),
    title VARCHAR(255) NOT NULL,
    status VARCHAR(40) NOT NULL CHECK (status IN ('DRAFT','FINAL_REVIEW','APPROVED','ARCHIVED')),
    citation_style VARCHAR(60) NOT NULL,
    content_json TEXT NOT NULL,
    plain_text TEXT,
    source_report_revision_number INTEGER NOT NULL,
    section_revision_snapshot_json TEXT,
    references_snapshot_json TEXT,
    template_snapshot_json TEXT,
    created_by UUID NOT NULL REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(report_id, version_number)
);

CREATE INDEX IF NOT EXISTS idx_report_document_versions_report
    ON report_document_versions(report_id, version_number DESC);

ALTER TABLE report_export_jobs
    ADD COLUMN IF NOT EXISTS final_document_version_id UUID REFERENCES report_document_versions(id);

ALTER TABLE research_reports DROP CONSTRAINT IF EXISTS research_reports_citation_style_check;
ALTER TABLE research_reports
    ADD CONSTRAINT research_reports_citation_style_check
        CHECK (citation_style IN ('APA_7','HARVARD','IEEE','CHICAGO_AUTHOR_DATE','VANCOUVER','MLA_9','CUSTOM','NUMERIC_APA'));

ALTER TABLE report_export_jobs DROP CONSTRAINT IF EXISTS report_export_jobs_citation_style_check;
ALTER TABLE report_export_jobs
    ADD CONSTRAINT report_export_jobs_citation_style_check
        CHECK (citation_style IN ('APA_7','HARVARD','IEEE','CHICAGO_AUTHOR_DATE','VANCOUVER','MLA_9','CUSTOM','NUMERIC_APA'));
