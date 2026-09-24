-- V41: Hierarchical report structure, nested subsections, and literature matrix separation

-- 1. Extend research_report_sections with hierarchy, numbering, and template flags
ALTER TABLE research_report_sections
    ADD COLUMN IF NOT EXISTS parent_section_id UUID REFERENCES research_report_sections(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS section_number VARCHAR(32),
    ADD COLUMN IF NOT EXISTS required BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS system_defined BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS ai_enabled BOOLEAN NOT NULL DEFAULT true;

CREATE INDEX IF NOT EXISTS idx_research_report_sections_parent
    ON research_report_sections(parent_section_id);

-- 2. Extend research_report_chapters with template flags
ALTER TABLE research_report_chapters
    ADD COLUMN IF NOT EXISTS required BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS system_defined BOOLEAN NOT NULL DEFAULT false;

-- 3. Extend research_reports with reference and matrix inclusion options
ALTER TABLE research_reports
    ADD COLUMN IF NOT EXISTS include_uncited_references BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS literature_matrix_inclusion VARCHAR(40) NOT NULL DEFAULT 'NONE';

-- 4. Extend literature_matrices to store structured table data
ALTER TABLE literature_matrices
    ADD COLUMN IF NOT EXISTS matrix_data_json TEXT,
    ADD COLUMN IF NOT EXISTS markdown_table TEXT,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

-- 5. Backfill existing chapters as system_defined and required where appropriate
UPDATE research_report_chapters
SET system_defined = true,
    required = (type IN ('PRELIMINARY', 'INTRODUCTION', 'LITERATURE_REVIEW', 'METHODOLOGY', 'RESULTS', 'CONCLUSION_RECOMMENDATIONS', 'REFERENCES'))
WHERE system_defined IS FALSE;

-- 6. Backfill existing sections as system_defined and required where appropriate
UPDATE research_report_sections
SET system_defined = true,
    required = (type IN ('ABSTRACT', 'PROBLEM_STATEMENT', 'OBJECTIVES', 'LITERATURE_REVIEW', 'METHODOLOGY', 'FINDINGS', 'CONCLUSIONS', 'REFERENCES'))
WHERE system_defined IS FALSE;

-- 7. Ensure any section in a REFERENCES chapter has type REFERENCES
UPDATE research_report_sections s
SET type = 'REFERENCES'
FROM research_report_chapters c
WHERE s.chapter_id = c.id
  AND (c.type = 'REFERENCES' OR LOWER(s.heading) = 'references')
  AND s.type != 'REFERENCES';
