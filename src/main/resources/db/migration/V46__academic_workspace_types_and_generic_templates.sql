-- ============================================================
-- V46 - Academic workspace types and generic template compatibility
-- ============================================================
--
-- Keeps research_projects as the durable academic work entity while
-- adding type metadata needed for Academic Research, Academic Project,
-- and Coursework workspaces. Existing rows are classified as
-- ACADEMIC_RESEARCH because the pre-migration product semantics and
-- modules are research-project oriented.
-- ============================================================

ALTER TABLE research_projects
    ADD COLUMN IF NOT EXISTS workspace_type VARCHAR(40) NOT NULL DEFAULT 'ACADEMIC_RESEARCH',
    ADD COLUMN IF NOT EXISTS academic_project_type VARCHAR(80),
    ADD COLUMN IF NOT EXISTS institution VARCHAR(255),
    ADD COLUMN IF NOT EXISTS department VARCHAR(255),
    ADD COLUMN IF NOT EXISTS programme VARCHAR(255),
    ADD COLUMN IF NOT EXISTS academic_year VARCHAR(40),
    ADD COLUMN IF NOT EXISTS supervisor VARCHAR(255),
    ADD COLUMN IF NOT EXISTS course_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS course_code VARCHAR(80),
    ADD COLUMN IF NOT EXISTS lecturer VARCHAR(255),
    ADD COLUMN IF NOT EXISTS deadline DATE;

UPDATE research_projects
SET workspace_type = 'ACADEMIC_RESEARCH'
WHERE workspace_type IS NULL OR workspace_type = '';

ALTER TABLE research_projects
    DROP CONSTRAINT IF EXISTS chk_research_projects_workspace_type,
    DROP CONSTRAINT IF EXISTS chk_research_projects_academic_project_type;

ALTER TABLE research_projects
    ADD CONSTRAINT chk_research_projects_workspace_type
        CHECK (workspace_type IN ('ACADEMIC_RESEARCH', 'ACADEMIC_PROJECT', 'COURSEWORK')),
    ADD CONSTRAINT chk_research_projects_academic_project_type
        CHECK (
            academic_project_type IS NULL OR
            academic_project_type IN (
                'SOFTWARE_SYSTEM_DEVELOPMENT',
                'ENGINEERING_PROJECT',
                'RESEARCH_BASED_PROJECT',
                'BUSINESS_PROJECT',
                'GENERAL_ACADEMIC_PROJECT',
                'OTHER'
            )
        );

CREATE INDEX IF NOT EXISTS idx_research_projects_workspace_type
    ON research_projects(workspace_type);

CREATE INDEX IF NOT EXISTS idx_research_projects_workspace_type_status
    ON research_projects(workspace_id, workspace_type, status);

ALTER TABLE research_projects
    DROP CONSTRAINT IF EXISTS chk_research_projects_status,
    DROP CONSTRAINT IF EXISTS research_projects_status_check;

ALTER TABLE research_projects
    ADD CONSTRAINT chk_research_projects_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'ON_HOLD', 'COMPLETED', 'ARCHIVED', 'TRASHED'));

ALTER TABLE research_report_templates
    ADD COLUMN IF NOT EXISTS supported_workspace_types VARCHAR(255);

ALTER TABLE research_report_templates
    DROP CONSTRAINT IF EXISTS research_report_templates_type_check,
    DROP CONSTRAINT IF EXISTS chk_research_report_templates_type;

ALTER TABLE research_report_templates
    ADD CONSTRAINT chk_research_report_templates_type
        CHECK (
            type IN (
                'FINAL_YEAR_PROJECT',
                'THESIS',
                'DISSERTATION',
                'RESEARCH_REPORT',
                'ACADEMIC_PROJECT_REPORT',
                'COURSEWORK',
                'JOURNAL_MANUSCRIPT',
                'JOURNAL_ARTICLE',
                'TECHNICAL_REPORT',
                'CUSTOM'
            )
        );

ALTER TABLE research_reports
    DROP CONSTRAINT IF EXISTS research_reports_type_check,
    DROP CONSTRAINT IF EXISTS chk_research_reports_type;

ALTER TABLE research_reports
    ADD CONSTRAINT chk_research_reports_type
        CHECK (
            type IN (
                'FINAL_YEAR_PROJECT',
                'THESIS',
                'DISSERTATION',
                'RESEARCH_REPORT',
                'ACADEMIC_PROJECT_REPORT',
                'COURSEWORK',
                'JOURNAL_MANUSCRIPT',
                'JOURNAL_ARTICLE',
                'TECHNICAL_REPORT',
                'CUSTOM'
            )
        );

UPDATE research_report_templates
SET supported_workspace_types = 'ACADEMIC_PROJECT'
WHERE type = 'FINAL_YEAR_PROJECT'
  AND (supported_workspace_types IS NULL OR supported_workspace_types = '');

UPDATE research_report_templates
SET supported_workspace_types = 'ACADEMIC_RESEARCH'
WHERE type = 'RESEARCH_REPORT'
  AND (supported_workspace_types IS NULL OR supported_workspace_types = '');

INSERT INTO research_report_templates(
    id,
    name,
    type,
    institution,
    department,
    supported_workspace_types,
    system_template,
    default_citation_style,
    citation_style_locked,
    description,
    configuration_json
) VALUES (
    '00000000-0000-0000-0000-000000000522',
    'Generic Academic Project Report',
    'ACADEMIC_PROJECT_REPORT',
    NULL,
    NULL,
    'ACADEMIC_PROJECT',
    TRUE,
    'APA_7',
    FALSE,
    'Generic project report structure for final-year projects, capstones, software projects, engineering projects, business projects, and institution-required academic projects.',
    '{"chapters":[' ||
      '{"type":"PRELIMINARY","title":"Preliminary Pages","sections":[' ||
        '{"type":"TITLE_PAGE","heading":"Title Page"},' ||
        '{"type":"ABSTRACT","heading":"Abstract"},' ||
        '{"type":"CUSTOM","heading":"Acknowledgements"},' ||
        '{"type":"CUSTOM","heading":"Table of Contents"},' ||
        '{"type":"CUSTOM","heading":"List of Figures"},' ||
        '{"type":"CUSTOM","heading":"List of Tables"}' ||
      ']},' ||
      '{"type":"INTRODUCTION","title":"Chapter One: Introduction","chapterNumber":1,"sections":[' ||
        '{"type":"BACKGROUND","heading":"Background and Context"},' ||
        '{"type":"PROBLEM_STATEMENT","heading":"Problem Statement"},' ||
        '{"type":"OBJECTIVES","heading":"Project Aim and Objectives"},' ||
        '{"type":"SIGNIFICANCE","heading":"Significance of the Project"},' ||
        '{"type":"SCOPE","heading":"Scope and Limitations"}' ||
      ']},' ||
      '{"type":"LITERATURE_REVIEW","title":"Chapter Two: Related Work","chapterNumber":2,"sections":[' ||
        '{"type":"LITERATURE_REVIEW","heading":"Review of Related Work"},' ||
        '{"type":"CONCEPTUAL_REVIEW","heading":"Conceptual and Technical Background"},' ||
        '{"type":"RESEARCH_GAP","heading":"Gap or Need Addressed"}' ||
      ']},' ||
      '{"type":"METHODOLOGY","title":"Chapter Three: Analysis and Design","chapterNumber":3,"sections":[' ||
        '{"type":"METHODOLOGY","heading":"Project Methodology"},' ||
        '{"type":"CUSTOM","heading":"Requirements or Design Criteria"},' ||
        '{"type":"CUSTOM","heading":"System, Process, or Solution Design"}' ||
      ']},' ||
      '{"type":"RESULTS","title":"Chapter Four: Implementation and Testing","chapterNumber":4,"sections":[' ||
        '{"type":"CUSTOM","heading":"Implementation"},' ||
        '{"type":"RESULTS","heading":"Testing or Evaluation Results"},' ||
        '{"type":"DISCUSSION","heading":"Discussion"}' ||
      ']},' ||
      '{"type":"CONCLUSION_RECOMMENDATIONS","title":"Chapter Five: Conclusion and Recommendations","chapterNumber":5,"sections":[' ||
        '{"type":"CONCLUSIONS","heading":"Conclusion"},' ||
        '{"type":"RECOMMENDATIONS","heading":"Recommendations and Future Work"}' ||
      ']},' ||
      '{"type":"REFERENCES","title":"References","sections":[{"type":"REFERENCES","heading":"References"}]},' ||
      '{"type":"APPENDICES","title":"Appendices","sections":[{"type":"APPENDIX","heading":"Appendix A"}]}' ||
    ']}'
) ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    type = EXCLUDED.type,
    supported_workspace_types = EXCLUDED.supported_workspace_types,
    description = EXCLUDED.description,
    configuration_json = EXCLUDED.configuration_json;

INSERT INTO research_report_templates(
    id,
    name,
    type,
    institution,
    department,
    supported_workspace_types,
    system_template,
    default_citation_style,
    citation_style_locked,
    description,
    configuration_json
) VALUES (
    '00000000-0000-0000-0000-000000000523',
    'Generic Coursework Document',
    'COURSEWORK',
    NULL,
    NULL,
    'COURSEWORK',
    TRUE,
    'APA_7',
    FALSE,
    'Generic coursework document for assignments, term papers, essays, case studies, and class exercises.',
    '{"chapters":[' ||
      '{"type":"PRELIMINARY","title":"Document Information","sections":[' ||
        '{"type":"TITLE_PAGE","heading":"Title Page"},' ||
        '{"type":"ABSTRACT","heading":"Summary or Abstract"}' ||
      ']},' ||
      '{"type":"INTRODUCTION","title":"Introduction","sections":[' ||
        '{"type":"BACKGROUND","heading":"Introduction"},' ||
        '{"type":"CUSTOM","heading":"Assignment Context"}' ||
      ']},' ||
      '{"type":"LITERATURE_REVIEW","title":"Discussion","sections":[' ||
        '{"type":"LITERATURE_REVIEW","heading":"Discussion and Analysis"},' ||
        '{"type":"CUSTOM","heading":"Supporting Evidence"}' ||
      ']},' ||
      '{"type":"CONCLUSION_RECOMMENDATIONS","title":"Conclusion","sections":[' ||
        '{"type":"CONCLUSIONS","heading":"Conclusion"}' ||
      ']},' ||
      '{"type":"REFERENCES","title":"References","sections":[{"type":"REFERENCES","heading":"References"}]},' ||
      '{"type":"APPENDICES","title":"Appendices","sections":[{"type":"APPENDIX","heading":"Appendix A"}]}' ||
    ']}'
) ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    type = EXCLUDED.type,
    supported_workspace_types = EXCLUDED.supported_workspace_types,
    description = EXCLUDED.description,
    configuration_json = EXCLUDED.configuration_json;
