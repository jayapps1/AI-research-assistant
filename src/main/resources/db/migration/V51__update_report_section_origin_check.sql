-- V51__update_report_section_origin_check.sql
-- Allow DETERMINISTIC_SYSTEM and TEMPLATE values in research_report_sections origin constraint.

ALTER TABLE research_report_sections
    DROP CONSTRAINT IF EXISTS research_report_sections_origin_check;

ALTER TABLE research_report_sections
    ADD CONSTRAINT research_report_sections_origin_check
    CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED','DETERMINISTIC_SYSTEM','TEMPLATE'));
