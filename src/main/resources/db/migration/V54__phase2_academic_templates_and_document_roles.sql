-- V54: Phase 2 - Template-guided academic document structure, distinct file roles,
-- section requirement levels, template guidance, and document guideline extraction/versioning.

-- 1. Add academic_role to documents table
ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS academic_role VARCHAR(60) NOT NULL DEFAULT 'RESEARCH_SOURCE';

CREATE INDEX IF NOT EXISTS idx_documents_academic_role ON documents(academic_role);

-- 2. Add requirement_level and template_guidance to research_report_sections table
ALTER TABLE research_report_sections
    ADD COLUMN IF NOT EXISTS requirement_level VARCHAR(30) NOT NULL DEFAULT 'REQUIRED',
    ADD COLUMN IF NOT EXISTS template_guidance TEXT;

CREATE INDEX IF NOT EXISTS idx_report_sections_requirement ON research_report_sections(requirement_level);

-- 3. Create academic_document_guidelines table for template upload, extraction, review, and versioning
CREATE TABLE IF NOT EXISTS academic_document_guidelines (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    project_id UUID REFERENCES research_projects(id) ON DELETE SET NULL,
    document_id UUID REFERENCES documents(id) ON DELETE SET NULL,
    original_file_name VARCHAR(255) NOT NULL,
    source VARCHAR(50) NOT NULL DEFAULT 'UPLOADED',
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL DEFAULT 'EXTRACTED',
    institution VARCHAR(255),
    department VARCHAR(255),
    programme VARCHAR(255),
    document_type VARCHAR(100),
    citation_style VARCHAR(60),
    raw_extraction_json TEXT,
    approved_structure_json TEXT,
    formatting_rules_json TEXT,
    uncertain_items_json TEXT,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    approved_at TIMESTAMP WITH TIME ZONE,
    approved_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_guidelines_project_id ON academic_document_guidelines(project_id);
CREATE INDEX IF NOT EXISTS idx_guidelines_workspace_id ON academic_document_guidelines(workspace_id);
CREATE INDEX IF NOT EXISTS idx_guidelines_status ON academic_document_guidelines(status);
