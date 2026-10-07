-- V49: Relax rigid unique constraints on display_order for chapters and sections
-- to support dynamic reordering, nested subsections, and drag-and-drop operations.

ALTER TABLE research_report_sections
    DROP CONSTRAINT IF EXISTS research_report_sections_chapter_id_display_order_key;

CREATE INDEX IF NOT EXISTS idx_research_report_sections_chapter_order
    ON research_report_sections(chapter_id, parent_section_id, display_order);

ALTER TABLE research_report_chapters
    DROP CONSTRAINT IF EXISTS research_report_chapters_report_id_display_order_key;

CREATE INDEX IF NOT EXISTS idx_research_report_chapters_report_order
    ON research_report_chapters(report_id, display_order);
