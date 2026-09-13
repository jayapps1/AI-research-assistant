-- ============================================================
-- AI RESEARCH ASSISTANT
-- V6 - CREATE WORKSPACES AND MEMBERSHIPS
-- ============================================================
--
-- Purpose:
-- Introduces workspaces as the tenant boundary for future
-- research projects, documents, RAG indexes and reports.
--
-- Security:
-- A user may access workspace-scoped data only through an ACTIVE
-- membership. Workspace ownership is represented both by
-- workspaces.owner_id and one OWNER membership for the same user.
-- ============================================================

CREATE TABLE workspaces
(
    id UUID PRIMARY KEY,

    name VARCHAR(255) NOT NULL,

    type VARCHAR(30) NOT NULL,

    owner_id UUID NOT NULL,

    status VARCHAR(30)
        NOT NULL
        DEFAULT 'ACTIVE',

    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_workspaces_owner
        FOREIGN KEY (owner_id)
        REFERENCES users(id),

    CONSTRAINT chk_workspaces_type
        CHECK (
            type IN (
                'PERSONAL',
                'ORGANIZATION'
            )
        ),

    CONSTRAINT chk_workspaces_status
        CHECK (
            status IN (
                'ACTIVE',
                'ARCHIVED',
                'SUSPENDED'
            )
        )
);

CREATE INDEX idx_workspaces_owner_id
    ON workspaces(owner_id);

CREATE INDEX idx_workspaces_status
    ON workspaces(status);

CREATE TABLE workspace_memberships
(
    id UUID PRIMARY KEY,

    workspace_id UUID NOT NULL,

    user_id UUID NOT NULL,

    role VARCHAR(30) NOT NULL,

    status VARCHAR(30) NOT NULL,

    joined_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_workspace_memberships_workspace
        FOREIGN KEY (workspace_id)
        REFERENCES workspaces(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_workspace_memberships_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT uk_workspace_memberships_workspace_user
        UNIQUE (workspace_id, user_id),

    CONSTRAINT chk_workspace_memberships_role
        CHECK (
            role IN (
                'OWNER',
                'ADMIN',
                'MEMBER'
            )
        ),

    CONSTRAINT chk_workspace_memberships_status
        CHECK (
            status IN (
                'ACTIVE',
                'INVITED',
                'SUSPENDED',
                'REMOVED'
            )
        )
);

CREATE UNIQUE INDEX uk_workspace_memberships_single_owner
    ON workspace_memberships(workspace_id)
    WHERE role = 'OWNER';

CREATE INDEX idx_workspace_memberships_workspace_id
    ON workspace_memberships(workspace_id);

CREATE INDEX idx_workspace_memberships_user_id
    ON workspace_memberships(user_id);

CREATE INDEX idx_workspace_memberships_workspace_status
    ON workspace_memberships(workspace_id, status);

CREATE INDEX idx_workspace_memberships_user_status
    ON workspace_memberships(user_id, status);
