-- V52__update_report_section_type_check.sql
-- Expand research_report_sections_type_check constraint to include all section types.

ALTER TABLE research_report_sections
    DROP CONSTRAINT IF EXISTS research_report_sections_type_check;

ALTER TABLE research_report_sections
    ADD CONSTRAINT research_report_sections_type_check
    CHECK (type IN (
        'TITLE_PAGE',
        'DECLARATION',
        'CERTIFICATION',
        'DEDICATION',
        'ACKNOWLEDGEMENTS',
        'ABSTRACT',
        'TABLE_OF_CONTENTS',
        'LIST_OF_FIGURES',
        'LIST_OF_TABLES',
        'BACKGROUND',
        'PROBLEM_STATEMENT',
        'OBJECTIVES',
        'RESEARCH_QUESTIONS',
        'HYPOTHESES',
        'SIGNIFICANCE',
        'SCOPE',
        'LITERATURE_REVIEW',
        'CONCEPTUAL_REVIEW',
        'THEORETICAL_REVIEW',
        'EMPIRICAL_REVIEW',
        'RESEARCH_GAP',
        'CONCEPTUAL_FRAMEWORK',
        'RELATED_SYSTEMS',
        'METHODOLOGY',
        'SYSTEM_REQUIREMENTS',
        'SYSTEM_DESIGN',
        'IMPLEMENTATION',
        'TESTING',
        'RESULTS',
        'FINDINGS',
        'DISCUSSION',
        'CONCLUSIONS',
        'RECOMMENDATIONS',
        'FUTURE_WORK',
        'REFERENCES',
        'APPENDIX',
        'CUSTOM'
    ));
