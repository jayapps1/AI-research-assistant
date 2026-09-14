ALTER TABLE research_findings
    ADD COLUMN IF NOT EXISTS type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS status VARCHAR(40),
    ADD COLUMN IF NOT EXISTS finding_text TEXT,
    ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS revision_number INTEGER,
    ADD COLUMN IF NOT EXISTS result_value_snapshot TEXT;

UPDATE research_findings
SET type = COALESCE(type, 'OTHER'),
    status = COALESCE(status, 'DRAFT'),
    finding_text = COALESCE(finding_text, statement),
    revision_number = COALESCE(revision_number, 1);

ALTER TABLE research_findings
    ALTER COLUMN analysis_result_id DROP NOT NULL,
    ALTER COLUMN type SET NOT NULL,
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN finding_text SET NOT NULL,
    ALTER COLUMN revision_number SET NOT NULL,
    ADD CONSTRAINT chk_research_findings_type CHECK (type IN ('QUANTITATIVE','QUALITATIVE','MIXED_METHODS','DESCRIPTIVE','INFERENTIAL','OTHER')),
    ADD CONSTRAINT chk_research_findings_status CHECK (status IN ('DRAFT','REVIEWED','APPROVED','SUPERSEDED','ARCHIVED')),
    ADD CONSTRAINT chk_research_findings_revision CHECK (revision_number >= 1);

CREATE TABLE research_finding_analysis_results (
    finding_id UUID NOT NULL REFERENCES research_findings(id) ON DELETE CASCADE,
    analysis_result_id UUID NOT NULL REFERENCES analysis_results(id) ON DELETE RESTRICT,
    relationship_type VARCHAR(60),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (finding_id, analysis_result_id)
);
CREATE INDEX idx_research_finding_analysis_results_result ON research_finding_analysis_results(analysis_result_id);

INSERT INTO research_finding_analysis_results(finding_id, analysis_result_id, relationship_type)
SELECT id, analysis_result_id, 'PRIMARY'
FROM research_findings
WHERE analysis_result_id IS NOT NULL
ON CONFLICT DO NOTHING;

CREATE TABLE research_finding_qualitative_themes (
    finding_id UUID NOT NULL REFERENCES research_findings(id) ON DELETE CASCADE,
    qualitative_theme_id UUID NOT NULL,
    theme_snapshot TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (finding_id, qualitative_theme_id)
);

CREATE TABLE research_finding_code_applications (
    finding_id UUID NOT NULL REFERENCES research_findings(id) ON DELETE CASCADE,
    qualitative_code_application_id UUID NOT NULL,
    source_snapshot TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (finding_id, qualitative_code_application_id)
);

ALTER TABLE finding_discussions
    ADD COLUMN IF NOT EXISTS title VARCHAR(255),
    ADD COLUMN IF NOT EXISTS discussion_text TEXT,
    ADD COLUMN IF NOT EXISTS status VARCHAR(40),
    ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS revision_number INTEGER;

UPDATE finding_discussions
SET discussion_text = COALESCE(discussion_text, interpretation),
    status = COALESCE(status, 'DRAFT'),
    revision_number = COALESCE(revision_number, 1);

ALTER TABLE finding_discussions
    ALTER COLUMN discussion_text SET NOT NULL,
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN revision_number SET NOT NULL,
    ADD CONSTRAINT chk_finding_discussions_status CHECK (status IN ('DRAFT','REVIEWED','APPROVED','SUPERSEDED','ARCHIVED')),
    ADD CONSTRAINT chk_finding_discussions_revision CHECK (revision_number >= 1);

CREATE TABLE discussion_evidence (
    id UUID PRIMARY KEY,
    discussion_id UUID NOT NULL REFERENCES finding_discussions(id) ON DELETE CASCADE,
    document_id UUID NOT NULL REFERENCES documents(id),
    document_version_id UUID NOT NULL REFERENCES document_versions(id),
    page_id UUID REFERENCES document_pages(id),
    chunk_id UUID REFERENCES document_chunks(id),
    rag_evidence_id UUID REFERENCES rag_query_evidence(id),
    evidence_snapshot TEXT NOT NULL,
    relationship VARCHAR(60) NOT NULL CHECK (relationship IN ('SUPPORTS','CONTRASTS','EXTENDS','CONTEXTUALIZES','METHODOLOGICAL_COMPARISON','THEORETICAL_ALIGNMENT','OTHER')),
    citation_ordinal INTEGER NOT NULL CHECK (citation_ordinal >= 1),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (discussion_id, citation_ordinal)
);
CREATE INDEX idx_discussion_evidence_discussion ON discussion_evidence(discussion_id);
CREATE INDEX idx_discussion_evidence_document ON discussion_evidence(document_id);

ALTER TABLE research_conclusions
    ADD COLUMN IF NOT EXISTS conclusion_text TEXT,
    ADD COLUMN IF NOT EXISTS status VARCHAR(40),
    ADD COLUMN IF NOT EXISTS type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS revision_number INTEGER;

UPDATE research_conclusions
SET conclusion_text = COALESCE(conclusion_text, statement),
    status = COALESCE(status, 'DRAFT'),
    type = COALESCE(type, 'OBJECTIVE_SPECIFIC'),
    revision_number = COALESCE(revision_number, 1);

ALTER TABLE research_conclusions
    ALTER COLUMN conclusion_text SET NOT NULL,
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN type SET NOT NULL,
    ALTER COLUMN revision_number SET NOT NULL,
    ADD CONSTRAINT chk_research_conclusions_status CHECK (status IN ('DRAFT','REVIEWED','APPROVED','SUPERSEDED','ARCHIVED')),
    ADD CONSTRAINT chk_research_conclusions_type CHECK (type IN ('OBJECTIVE_SPECIFIC','OVERALL')),
    ADD CONSTRAINT chk_research_conclusions_revision CHECK (revision_number >= 1);

CREATE TABLE research_conclusion_findings (
    conclusion_id UUID NOT NULL REFERENCES research_conclusions(id) ON DELETE CASCADE,
    finding_id UUID NOT NULL REFERENCES research_findings(id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (conclusion_id, finding_id)
);
CREATE INDEX idx_research_conclusion_findings_finding ON research_conclusion_findings(finding_id);

ALTER TABLE research_recommendations
    ADD COLUMN IF NOT EXISTS type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS recommendation_text TEXT,
    ADD COLUMN IF NOT EXISTS target_audience VARCHAR(255),
    ADD COLUMN IF NOT EXISTS status VARCHAR(40),
    ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS revision_number INTEGER;

UPDATE research_recommendations
SET type = COALESCE(type, 'OTHER'),
    recommendation_text = COALESCE(recommendation_text, recommendation),
    target_audience = COALESCE(target_audience, audience),
    status = COALESCE(status, 'DRAFT'),
    revision_number = COALESCE(revision_number, 1);

ALTER TABLE research_recommendations
    ALTER COLUMN type SET NOT NULL,
    ALTER COLUMN recommendation_text SET NOT NULL,
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN revision_number SET NOT NULL,
    ADD CONSTRAINT chk_research_recommendations_type CHECK (type IN ('PRACTICE','POLICY','MANAGEMENT','EDUCATION','FUTURE_RESEARCH','TECHNICAL','COMMUNITY','INSTITUTIONAL','OTHER')),
    ADD CONSTRAINT chk_research_recommendations_status CHECK (status IN ('DRAFT','REVIEWED','APPROVED','SUPERSEDED','ARCHIVED')),
    ADD CONSTRAINT chk_research_recommendations_revision CHECK (revision_number >= 1);

CREATE TABLE research_recommendation_findings (
    recommendation_id UUID NOT NULL REFERENCES research_recommendations(id) ON DELETE CASCADE,
    finding_id UUID NOT NULL REFERENCES research_findings(id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (recommendation_id, finding_id)
);

CREATE TABLE research_recommendation_conclusions (
    recommendation_id UUID NOT NULL REFERENCES research_recommendations(id) ON DELETE CASCADE,
    conclusion_id UUID NOT NULL REFERENCES research_conclusions(id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (recommendation_id, conclusion_id)
);

CREATE TABLE research_recommendation_objectives (
    recommendation_id UUID NOT NULL REFERENCES research_recommendations(id) ON DELETE CASCADE,
    objective_id UUID NOT NULL REFERENCES research_objectives(id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (recommendation_id, objective_id)
);

CREATE TABLE research_report_templates (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(60) NOT NULL CHECK (type IN ('FINAL_YEAR_PROJECT','THESIS','DISSERTATION','RESEARCH_REPORT','JOURNAL_MANUSCRIPT','TECHNICAL_REPORT','CUSTOM')),
    institution VARCHAR(255),
    system_template BOOLEAN NOT NULL DEFAULT FALSE,
    configuration_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE research_reports (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    template_id UUID REFERENCES research_report_templates(id),
    title VARCHAR(255) NOT NULL,
    type VARCHAR(60) NOT NULL CHECK (type IN ('FINAL_YEAR_PROJECT','THESIS','DISSERTATION','RESEARCH_REPORT','JOURNAL_MANUSCRIPT','TECHNICAL_REPORT','CUSTOM')),
    status VARCHAR(40) NOT NULL CHECK (status IN ('DRAFT','IN_REVIEW','APPROVED','FINAL','SUPERSEDED','ARCHIVED')),
    institution_name VARCHAR(255),
    department_name VARCHAR(255),
    author_name VARCHAR(255),
    supervisor_name VARCHAR(255),
    degree_program VARCHAR(255),
    submission_year INTEGER,
    citation_style VARCHAR(60) NOT NULL CHECK (citation_style IN ('APA_7','HARVARD','IEEE','CHICAGO_AUTHOR_DATE','VANCOUVER','MLA_9','CUSTOM')),
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    revision_number INTEGER NOT NULL CHECK (revision_number >= 1),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_research_reports_project ON research_reports(project_id);

CREATE TABLE research_report_chapters (
    id UUID PRIMARY KEY,
    report_id UUID NOT NULL REFERENCES research_reports(id) ON DELETE CASCADE,
    type VARCHAR(60) NOT NULL CHECK (type IN ('PRELIMINARY','INTRODUCTION','LITERATURE_REVIEW','METHODOLOGY','RESULTS','DISCUSSION','CONCLUSION_RECOMMENDATIONS','REFERENCES','APPENDICES','CUSTOM')),
    title VARCHAR(255) NOT NULL,
    chapter_number INTEGER CHECK (chapter_number IS NULL OR chapter_number >= 1),
    display_order INTEGER NOT NULL CHECK (display_order >= 1),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(report_id, display_order)
);

CREATE TABLE research_report_sections (
    id UUID PRIMARY KEY,
    chapter_id UUID NOT NULL REFERENCES research_report_chapters(id) ON DELETE CASCADE,
    type VARCHAR(80) NOT NULL CHECK (type IN ('TITLE_PAGE','ABSTRACT','BACKGROUND','PROBLEM_STATEMENT','OBJECTIVES','RESEARCH_QUESTIONS','HYPOTHESES','SIGNIFICANCE','SCOPE','LITERATURE_REVIEW','CONCEPTUAL_REVIEW','THEORETICAL_REVIEW','EMPIRICAL_REVIEW','RESEARCH_GAP','CONCEPTUAL_FRAMEWORK','METHODOLOGY','RESULTS','FINDINGS','DISCUSSION','CONCLUSIONS','RECOMMENDATIONS','REFERENCES','APPENDIX','CUSTOM')),
    heading VARCHAR(255) NOT NULL,
    content TEXT,
    display_order INTEGER NOT NULL CHECK (display_order >= 1),
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    source_artifact_type VARCHAR(100),
    source_artifact_id UUID,
    source_revision_number INTEGER,
    source_out_of_date BOOLEAN NOT NULL DEFAULT FALSE,
    manually_edited BOOLEAN NOT NULL DEFAULT FALSE,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    revision_number INTEGER NOT NULL CHECK (revision_number >= 1),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(chapter_id, display_order)
);

CREATE TABLE research_report_citations (
    id UUID PRIMARY KEY,
    section_id UUID NOT NULL REFERENCES research_report_sections(id) ON DELETE CASCADE,
    document_id UUID NOT NULL REFERENCES documents(id),
    document_version_id UUID NOT NULL REFERENCES document_versions(id),
    page_id UUID REFERENCES document_pages(id),
    chunk_id UUID REFERENCES document_chunks(id),
    document_code VARCHAR(32) NOT NULL,
    citation_ordinal INTEGER NOT NULL CHECK (citation_ordinal >= 1),
    supporting_text_snapshot TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(section_id, citation_ordinal)
);

CREATE TABLE research_report_artifact_references (
    id UUID PRIMARY KEY,
    report_id UUID NOT NULL REFERENCES research_reports(id) ON DELETE CASCADE,
    section_id UUID REFERENCES research_report_sections(id) ON DELETE CASCADE,
    artifact_type VARCHAR(100) NOT NULL,
    artifact_id UUID NOT NULL,
    artifact_revision_number INTEGER,
    label VARCHAR(255),
    reference_kind VARCHAR(40) NOT NULL CHECK (reference_kind IN ('TABLE','FIGURE','APPENDIX','SOURCE')),
    display_order INTEGER NOT NULL CHECK (display_order >= 1),
    allocated_number VARCHAR(40),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO research_report_templates(id, name, type, institution, system_template, configuration_json)
VALUES (
    '00000000-0000-0000-0000-000000000516',
    'Default Final Year Project',
    'FINAL_YEAR_PROJECT',
    NULL,
    TRUE,
    '{"chapters":[{"type":"PRELIMINARY","title":"Preliminary Pages"},{"type":"INTRODUCTION","title":"Chapter One: Introduction"},{"type":"LITERATURE_REVIEW","title":"Chapter Two: Literature Review"},{"type":"METHODOLOGY","title":"Chapter Three: Methodology"},{"type":"RESULTS","title":"Chapter Four: Results / Findings"},{"type":"DISCUSSION","title":"Chapter Five: Discussion, Conclusions and Recommendations"},{"type":"REFERENCES","title":"References"},{"type":"APPENDICES","title":"Appendices"}]}'
) ON CONFLICT DO NOTHING;
