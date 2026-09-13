-- ============================================================
-- AI RESEARCH ASSISTANT
-- V7 - CREATE RESEARCH PROJECTS AND MEMBERSHIPS
-- ============================================================
--
-- Purpose:
-- Introduces research projects under workspaces and project-level
-- membership for selective access within a workspace.
--
-- Document numbering:
-- next_document_number is allocation state, not a document count.
-- It must increase monotonically and must never be reset or derived
-- from existing document rows. Future document creation will lock the
-- project row, reserve this value, increment it, and commit.
-- ============================================================

CREATE TABLE research_projects
(
    id UUID PRIMARY KEY,

    workspace_id UUID NOT NULL,

    title VARCHAR(255) NOT NULL,

    description TEXT,

    status VARCHAR(30) NOT NULL,

    created_by UUID NOT NULL,

    next_document_number BIGINT
        NOT NULL
        DEFAULT 1,

    version BIGINT,

    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_research_projects_workspace
        FOREIGN KEY (workspace_id)
        REFERENCES workspaces(id),

    CONSTRAINT fk_research_projects_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT chk_research_projects_status
        CHECK (
            status IN (
                'DRAFT',
                'ACTIVE',
                'COMPLETED',
                'ARCHIVED'
            )
        ),

    CONSTRAINT chk_research_projects_next_document_number
        CHECK (next_document_number >= 1)
);

CREATE INDEX idx_research_projects_workspace_id
    ON research_projects(workspace_id);

CREATE INDEX idx_research_projects_created_by
    ON research_projects(created_by);

CREATE INDEX idx_research_projects_status
    ON research_projects(status);

CREATE INDEX idx_research_projects_workspace_status
    ON research_projects(workspace_id, status);

CREATE TABLE project_memberships
(
    id UUID PRIMARY KEY,

    project_id UUID NOT NULL,

    user_id UUID NOT NULL,

    role VARCHAR(30) NOT NULL,

    status VARCHAR(30) NOT NULL,

    added_by UUID,

    joined_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_project_memberships_project
        FOREIGN KEY (project_id)
        REFERENCES research_projects(id),

    CONSTRAINT fk_project_memberships_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_project_memberships_added_by
        FOREIGN KEY (added_by)
        REFERENCES users(id),

    CONSTRAINT uk_project_memberships_project_user
        UNIQUE (project_id, user_id),

    CONSTRAINT chk_project_memberships_role
        CHECK (
            role IN (
                'LEAD',
                'EDITOR',
                'VIEWER'
            )
        ),

    CONSTRAINT chk_project_memberships_status
        CHECK (
            status IN (
                'ACTIVE',
                'INVITED',
                'SUSPENDED',
                'REMOVED'
            )
        )
);

CREATE INDEX idx_project_memberships_project_id
    ON project_memberships(project_id);

CREATE INDEX idx_project_memberships_user_id
    ON project_memberships(user_id);

CREATE INDEX idx_project_memberships_project_status
    ON project_memberships(project_id, status);

CREATE INDEX idx_project_memberships_user_status
    ON project_memberships(user_id, status);

CREATE INDEX idx_project_memberships_project_role_status
    ON project_memberships(project_id, role, status);
