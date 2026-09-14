-- ============================================================
-- AI RESEARCH ASSISTANT
-- V19 - PROJECT COLLABORATION WORKFLOWS
-- ============================================================

ALTER TABLE project_memberships
    DROP CONSTRAINT chk_project_memberships_role;

ALTER TABLE project_memberships
    ADD CONSTRAINT chk_project_memberships_role
        CHECK (role IN ('LEAD', 'EDITOR', 'REVIEWER', 'SUPERVISOR', 'VIEWER'));

CREATE TABLE project_invitations
(
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    invited_email_normalized VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    invited_by UUID NOT NULL REFERENCES users(id),
    token_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    declined_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_project_invitations_role CHECK (role IN ('LEAD', 'EDITOR', 'REVIEWER', 'SUPERVISOR', 'VIEWER')),
    CONSTRAINT chk_project_invitations_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'EXPIRED', 'REVOKED')),
    CONSTRAINT chk_project_invitations_expiry CHECK (expires_at > created_at)
);

CREATE UNIQUE INDEX uk_project_invitations_pending_email
    ON project_invitations(project_id, invited_email_normalized)
    WHERE status = 'PENDING';
CREATE INDEX idx_project_invitations_project_status ON project_invitations(project_id, status);
CREATE INDEX idx_project_invitations_email_status ON project_invitations(invited_email_normalized, status);

CREATE TABLE research_report_authors
(
    id UUID PRIMARY KEY,
    report_id UUID NOT NULL REFERENCES research_reports(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id),
    display_name VARCHAR(255) NOT NULL,
    student_number VARCHAR(100),
    index_number VARCHAR(100),
    programme VARCHAR(255),
    author_order INTEGER NOT NULL,
    corresponding_author BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_report_authors_order CHECK (author_order >= 1),
    CONSTRAINT uk_report_authors_report_order UNIQUE (report_id, author_order)
);

CREATE INDEX idx_report_authors_report_order ON research_report_authors(report_id, author_order);

CREATE TABLE project_tasks
(
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL,
    priority VARCHAR(30) NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id),
    assigned_by UUID REFERENCES users(id),
    due_date DATE,
    due_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_project_tasks_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'IN_REVIEW', 'BLOCKED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_project_tasks_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT'))
);

CREATE INDEX idx_project_tasks_project_status ON project_tasks(project_id, status);
CREATE INDEX idx_project_tasks_project_priority ON project_tasks(project_id, priority);

CREATE TABLE project_task_assignees
(
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES project_tasks(id) ON DELETE CASCADE,
    project_membership_id UUID NOT NULL REFERENCES project_memberships(id),
    user_id UUID NOT NULL REFERENCES users(id),
    assigned_by UUID NOT NULL REFERENCES users(id),
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_task_assignee_membership UNIQUE (task_id, project_membership_id)
);

CREATE INDEX idx_project_task_assignees_user ON project_task_assignees(user_id);

CREATE TABLE project_task_artifact_links
(
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES project_tasks(id) ON DELETE CASCADE,
    artifact_type VARCHAR(80) NOT NULL,
    artifact_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_task_artifact_links_artifact ON project_task_artifact_links(artifact_type, artifact_id);

CREATE TABLE artifact_reviews
(
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    artifact_type VARCHAR(80) NOT NULL,
    artifact_id UUID NOT NULL,
    artifact_revision INTEGER NOT NULL,
    requested_by UUID NOT NULL REFERENCES users(id),
    reviewer_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(30) NOT NULL,
    summary TEXT,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_artifact_reviews_revision CHECK (artifact_revision >= 1),
    CONSTRAINT chk_artifact_reviews_status CHECK (status IN ('REQUESTED', 'IN_REVIEW', 'APPROVED', 'CHANGES_REQUESTED', 'CANCELLED', 'STALE'))
);

CREATE INDEX idx_artifact_reviews_project_status ON artifact_reviews(project_id, status);
CREATE INDEX idx_artifact_reviews_artifact ON artifact_reviews(project_id, artifact_type, artifact_id);

CREATE TABLE artifact_comments
(
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    artifact_type VARCHAR(80) NOT NULL,
    artifact_id UUID NOT NULL,
    author_id UUID NOT NULL REFERENCES users(id),
    parent_comment_id UUID REFERENCES artifact_comments(id),
    review_id UUID REFERENCES artifact_reviews(id),
    content TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    resolved_by UUID REFERENCES users(id),
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_artifact_comments_status CHECK (status IN ('OPEN', 'RESOLVED', 'DELETED'))
);

CREATE INDEX idx_artifact_comments_artifact ON artifact_comments(project_id, artifact_type, artifact_id);
CREATE INDEX idx_artifact_comments_parent ON artifact_comments(parent_comment_id);

CREATE TABLE artifact_comment_mentions
(
    id UUID PRIMARY KEY,
    comment_id UUID NOT NULL REFERENCES artifact_comments(id) ON DELETE CASCADE,
    mentioned_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_comment_mentioned_user UNIQUE (comment_id, mentioned_user_id)
);

CREATE INDEX idx_comment_mentions_user ON artifact_comment_mentions(mentioned_user_id);

CREATE TABLE project_activities
(
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    actor_id UUID REFERENCES users(id),
    type VARCHAR(80) NOT NULL,
    artifact_type VARCHAR(80),
    artifact_id UUID,
    safe_summary VARCHAR(500),
    metadata_json TEXT,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_project_activities_project_time ON project_activities(project_id, occurred_at);
CREATE INDEX idx_project_activities_actor ON project_activities(actor_id);
CREATE INDEX idx_project_activities_type ON project_activities(type);

CREATE TABLE artifact_approvals
(
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    artifact_type VARCHAR(80) NOT NULL,
    artifact_id UUID NOT NULL,
    artifact_revision INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    submitted_by UUID NOT NULL REFERENCES users(id),
    decided_by UUID REFERENCES users(id),
    decision_comment TEXT,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_artifact_approvals_revision CHECK (artifact_revision >= 1),
    CONSTRAINT chk_artifact_approvals_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'CHANGES_REQUESTED', 'WITHDRAWN', 'STALE'))
);

CREATE INDEX idx_artifact_approvals_artifact ON artifact_approvals(project_id, artifact_type, artifact_id);

CREATE TABLE project_collaboration_policies
(
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id) ON DELETE CASCADE,
    require_review_before_approval BOOLEAN NOT NULL DEFAULT FALSE,
    allow_lead_self_approval BOOLEAN NOT NULL DEFAULT TRUE,
    supervisor_approval_required BOOLEAN NOT NULL DEFAULT FALSE,
    minimum_reviewers INTEGER NOT NULL DEFAULT 0,
    allow_editors_invite_members BOOLEAN NOT NULL DEFAULT FALSE,
    comments_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    task_assignments_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_collaboration_policy_project UNIQUE (project_id),
    CONSTRAINT chk_collaboration_policy_min_reviewers CHECK (minimum_reviewers >= 0)
);

CREATE TABLE ai_artifact_proposals
(
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    artifact_type VARCHAR(80) NOT NULL,
    artifact_id UUID NOT NULL,
    based_on_revision INTEGER,
    ai_request_id UUID REFERENCES ai_requests(id),
    proposed_content TEXT,
    status VARCHAR(30) NOT NULL,
    requested_by UUID NOT NULL REFERENCES users(id),
    accepted_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_ai_artifact_proposals_revision CHECK (based_on_revision IS NULL OR based_on_revision >= 1),
    CONSTRAINT chk_ai_artifact_proposals_status CHECK (status IN ('GENERATED', 'ACCEPTED', 'REJECTED', 'STALE'))
);

CREATE INDEX idx_ai_artifact_proposals_artifact ON ai_artifact_proposals(project_id, artifact_type, artifact_id);
