-- ============================================================
-- AI RESEARCH ASSISTANT
-- V31 - ADD PROJECT RESEARCH SETUP FIELDS
-- ============================================================
--
-- Purpose:
-- Adds project-level research setup fields to research_projects
-- to support the simplified AI research setup flow:
-- research_aim, study_area, research_type, keywords.
-- ============================================================

ALTER TABLE research_projects
    ADD COLUMN IF NOT EXISTS research_aim TEXT,
    ADD COLUMN IF NOT EXISTS study_area VARCHAR(255),
    ADD COLUMN IF NOT EXISTS research_type VARCHAR(100),
    ADD COLUMN IF NOT EXISTS keywords VARCHAR(500);
