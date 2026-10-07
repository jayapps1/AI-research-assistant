-- V55: Phase 3 - Project Evidence, Figures, Screenshots, Diagrams, Tables, and Test Results

CREATE TABLE IF NOT EXISTS project_evidence (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id) ON DELETE CASCADE,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    report_id UUID REFERENCES research_reports(id) ON DELETE SET NULL,
    section_id UUID REFERENCES research_report_sections(id) ON DELETE SET NULL,
    storage_object_id UUID REFERENCES storage_objects(id) ON DELETE SET NULL,
    storage_key VARCHAR(512),
    original_filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(128) NOT NULL,
    file_size_bytes BIGINT NOT NULL DEFAULT 0,
    evidence_type VARCHAR(64) NOT NULL,
    figure_label VARCHAR(100),
    caption TEXT,
    description TEXT,
    alt_text TEXT,
    display_order INT NOT NULL DEFAULT 0,
    ai_visual_analysis TEXT,
    ai_analysis_status VARCHAR(32) NOT NULL DEFAULT 'NOT_ANALYZED',
    ai_analysis_error TEXT,
    metadata_json TEXT,
    created_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_project_evidence_project ON project_evidence(project_id);
CREATE INDEX IF NOT EXISTS idx_project_evidence_workspace ON project_evidence(workspace_id);
CREATE INDEX IF NOT EXISTS idx_project_evidence_report ON project_evidence(report_id);
CREATE INDEX IF NOT EXISTS idx_project_evidence_section ON project_evidence(section_id);
CREATE INDEX IF NOT EXISTS idx_project_evidence_type ON project_evidence(evidence_type);
CREATE INDEX IF NOT EXISTS idx_project_evidence_order ON project_evidence(project_id, display_order);
