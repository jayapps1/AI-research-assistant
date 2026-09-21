ALTER TABLE research_projects
    DROP CONSTRAINT IF EXISTS chk_research_projects_status;

ALTER TABLE research_projects
    ADD CONSTRAINT chk_research_projects_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'COMPLETED', 'ARCHIVED', 'TRASHED'));
