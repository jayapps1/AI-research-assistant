-- V50__section_semantic_purpose_and_generation_policy.sql
-- Adds semantic purpose and generation policy columns to research_report_sections,
-- disables AI for deterministic front-matter and bibliography nodes, and fixes legacy corrupted drafts.

ALTER TABLE research_report_sections
    ADD COLUMN IF NOT EXISTS semantic_purpose VARCHAR(80),
    ADD COLUMN IF NOT EXISTS generation_policy VARCHAR(80);

-- 1. Populate semantic_purpose and generation_policy from existing types
UPDATE research_report_sections
SET semantic_purpose = CASE
    WHEN type = 'TITLE_PAGE' THEN 'TITLE_PAGE'
    WHEN type = 'TABLE_OF_CONTENTS' THEN 'TABLE_OF_CONTENTS'
    WHEN type = 'LIST_OF_FIGURES' THEN 'LIST_OF_FIGURES'
    WHEN type = 'LIST_OF_TABLES' THEN 'LIST_OF_TABLES'
    WHEN type = 'DECLARATION' THEN 'DECLARATION'
    WHEN type = 'CERTIFICATION' THEN 'CERTIFICATION'
    WHEN type = 'DEDICATION' THEN 'DEDICATION'
    WHEN type = 'ACKNOWLEDGEMENTS' THEN 'ACKNOWLEDGEMENTS'
    WHEN type = 'ABSTRACT' THEN 'ABSTRACT'
    WHEN type = 'BACKGROUND' THEN 'BACKGROUND'
    WHEN type = 'PROBLEM_STATEMENT' THEN 'PROBLEM_STATEMENT'
    WHEN type = 'OBJECTIVES' THEN 'OBJECTIVES'
    WHEN type = 'RESEARCH_QUESTIONS' THEN 'RESEARCH_QUESTIONS'
    WHEN type = 'SIGNIFICANCE' THEN 'SIGNIFICANCE'
    WHEN type = 'SCOPE' THEN 'SCOPE'
    WHEN type IN ('LITERATURE_REVIEW', 'CONCEPTUAL_REVIEW', 'THEORETICAL_REVIEW', 'EMPIRICAL_REVIEW', 'RESEARCH_GAP', 'CONCEPTUAL_FRAMEWORK') THEN 'LITERATURE_REVIEW'
    WHEN type = 'RELATED_SYSTEMS' THEN 'RELATED_SYSTEMS'
    WHEN type = 'METHODOLOGY' THEN 'METHODOLOGY'
    WHEN type = 'SYSTEM_REQUIREMENTS' THEN 'SYSTEM_REQUIREMENTS'
    WHEN type = 'SYSTEM_DESIGN' THEN 'SYSTEM_DESIGN'
    WHEN type = 'IMPLEMENTATION' THEN 'IMPLEMENTATION'
    WHEN type = 'TESTING' THEN 'TESTING'
    WHEN type IN ('RESULTS', 'FINDINGS') THEN 'FINDINGS'
    WHEN type = 'DISCUSSION' THEN 'DISCUSSION'
    WHEN type = 'CONCLUSIONS' THEN 'CONCLUSIONS'
    WHEN type IN ('RECOMMENDATIONS', 'FUTURE_WORK') THEN 'RECOMMENDATIONS'
    WHEN type = 'REFERENCES' THEN 'REFERENCES'
    WHEN type = 'APPENDIX' THEN 'APPENDIX'
    ELSE 'CUSTOM'
END
WHERE semantic_purpose IS NULL;

-- 2. Populate preliminary custom sections with actual semantic purposes based on heading
UPDATE research_report_sections s
SET semantic_purpose = CASE
    WHEN LOWER(s.heading) LIKE '%title page%' THEN 'TITLE_PAGE'
    WHEN LOWER(s.heading) LIKE '%table of contents%' OR LOWER(s.heading) = 'contents' THEN 'TABLE_OF_CONTENTS'
    WHEN LOWER(s.heading) LIKE '%list of figures%' THEN 'LIST_OF_FIGURES'
    WHEN LOWER(s.heading) LIKE '%list of tables%' THEN 'LIST_OF_TABLES'
    WHEN LOWER(s.heading) LIKE '%declaration%' THEN 'DECLARATION'
    WHEN LOWER(s.heading) LIKE '%certification%' OR LOWER(s.heading) LIKE '%approval%' THEN 'CERTIFICATION'
    WHEN LOWER(s.heading) LIKE '%dedication%' THEN 'DEDICATION'
    WHEN LOWER(s.heading) LIKE '%acknowledgement%' OR LOWER(s.heading) LIKE '%acknowledgment%' THEN 'ACKNOWLEDGEMENTS'
    WHEN LOWER(s.heading) LIKE '%abstract%' THEN 'ABSTRACT'
    ELSE s.semantic_purpose
END
FROM research_report_chapters c
WHERE s.chapter_id = c.id
  AND c.type = 'PRELIMINARY'
  AND (s.semantic_purpose = 'CUSTOM' OR s.semantic_purpose IS NULL);

-- 3. Populate generation_policy from semantic_purpose
UPDATE research_report_sections
SET generation_policy = CASE
    WHEN semantic_purpose IN ('TITLE_PAGE', 'TABLE_OF_CONTENTS', 'LIST_OF_FIGURES', 'LIST_OF_TABLES', 'REFERENCES') THEN 'DETERMINISTIC'
    WHEN semantic_purpose IN ('DECLARATION', 'CERTIFICATION', 'DEDICATION', 'ACKNOWLEDGEMENTS', 'APPENDIX') THEN 'USER_AUTHORED_FRONT_MATTER'
    WHEN semantic_purpose = 'LITERATURE_REVIEW' THEN 'SOURCE_GROUNDED_AI'
    WHEN semantic_purpose IN ('TESTING', 'FINDINGS', 'DISCUSSION') THEN 'PROJECT_EVIDENCE_REQUIRED'
    WHEN semantic_purpose IN ('BACKGROUND', 'PROBLEM_STATEMENT', 'OBJECTIVES', 'RESEARCH_QUESTIONS', 'SIGNIFICANCE', 'SCOPE', 'RELATED_SYSTEMS', 'METHODOLOGY', 'SYSTEM_REQUIREMENTS', 'SYSTEM_DESIGN', 'IMPLEMENTATION', 'CONCLUSIONS', 'RECOMMENDATIONS', 'ABSTRACT') THEN 'PROJECT_DERIVED_AI'
    ELSE 'CONTEXTUAL_AI'
END
WHERE generation_policy IS NULL;

-- 4. Enforce ai_enabled = false on all deterministic sections
UPDATE research_report_sections
SET ai_enabled = FALSE
WHERE generation_policy = 'DETERMINISTIC'
   OR semantic_purpose IN ('TITLE_PAGE', 'TABLE_OF_CONTENTS', 'LIST_OF_FIGURES', 'LIST_OF_TABLES', 'REFERENCES');

-- 5. Delete corrupted citations created on deterministic sections
DELETE FROM research_report_citations
WHERE section_id IN (
    SELECT id FROM research_report_sections
    WHERE generation_policy = 'DETERMINISTIC'
       OR semantic_purpose IN ('TITLE_PAGE', 'TABLE_OF_CONTENTS', 'LIST_OF_FIGURES', 'LIST_OF_TABLES')
);
