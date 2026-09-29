-- ============================================================
-- AI RESEARCH ASSISTANT
-- V45 - GOOGLE DRIVE OBJECT STORAGE METADATA
-- ============================================================
--
-- PostgreSQL remains the application authority for ownership,
-- authorization, lifecycle status and provider object ids. Existing
-- local storage_key columns remain readable and are not migrated
-- automatically during startup.
-- ============================================================

CREATE TABLE IF NOT EXISTS storage_objects
(
    id UUID PRIMARY KEY,

    owner_user_id UUID NOT NULL REFERENCES users(id),
    workspace_id UUID REFERENCES workspaces(id) ON DELETE SET NULL,
    project_id UUID REFERENCES research_projects(id) ON DELETE SET NULL,

    provider VARCHAR(40) NOT NULL,
    provider_file_id VARCHAR(500),
    provider_parent_id VARCHAR(500),

    category VARCHAR(60) NOT NULL,
    storage_key VARCHAR(1000) NOT NULL,
    original_filename VARCHAR(500),
    stored_filename VARCHAR(500) NOT NULL,
    media_type VARCHAR(255),
    size_bytes BIGINT NOT NULL DEFAULT 0,
    checksum_sha256 VARCHAR(64),

    status VARCHAR(40) NOT NULL,
    failure_code VARCHAR(100),
    failure_message VARCHAR(1000),

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_storage_objects_provider_key
        UNIQUE (provider, storage_key),

    CONSTRAINT chk_storage_objects_provider
        CHECK (provider IN ('LOCAL', 'GOOGLE_DRIVE')),

    CONSTRAINT chk_storage_objects_category
        CHECK (category IN (
            'GENERAL',
            'RESEARCH_DOCUMENT',
            'DATASET',
            'IMAGE',
            'CONVERSATION_ATTACHMENT',
            'PROJECT_ATTACHMENT',
            'REPORT_EXPORT',
            'PROFILE_IMAGE',
            'OTHER'
        )),

    CONSTRAINT chk_storage_objects_status
        CHECK (status IN (
            'PENDING',
            'AVAILABLE',
            'FAILED',
            'TRASHED',
            'DELETED'
        )),

    CONSTRAINT chk_storage_objects_size
        CHECK (size_bytes >= 0)
);

CREATE INDEX IF NOT EXISTS idx_storage_objects_owner
    ON storage_objects(owner_user_id);

CREATE INDEX IF NOT EXISTS idx_storage_objects_workspace
    ON storage_objects(workspace_id);

CREATE INDEX IF NOT EXISTS idx_storage_objects_project
    ON storage_objects(project_id);

CREATE INDEX IF NOT EXISTS idx_storage_objects_provider_file
    ON storage_objects(provider, provider_file_id);

CREATE INDEX IF NOT EXISTS idx_storage_objects_status
    ON storage_objects(status);

ALTER TABLE document_versions
    ADD COLUMN IF NOT EXISTS storage_object_id UUID REFERENCES storage_objects(id);

CREATE INDEX IF NOT EXISTS idx_document_versions_storage_object
    ON document_versions(storage_object_id);

ALTER TABLE report_export_jobs
    ADD COLUMN IF NOT EXISTS storage_object_id UUID REFERENCES storage_objects(id);

CREATE INDEX IF NOT EXISTS idx_report_export_jobs_storage_object
    ON report_export_jobs(storage_object_id);

ALTER TABLE user_profile_images
    ADD COLUMN IF NOT EXISTS storage_object_id UUID REFERENCES storage_objects(id);

CREATE INDEX IF NOT EXISTS idx_user_profile_images_storage_object
    ON user_profile_images(storage_object_id);

CREATE TABLE IF NOT EXISTS conversation_attachments
(
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    storage_object_id UUID NOT NULL REFERENCES storage_objects(id),
    uploaded_by UUID NOT NULL REFERENCES users(id),
    original_filename VARCHAR(500) NOT NULL,
    media_type VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64),
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_conversation_attachments_status
        CHECK (status IN ('ACTIVE', 'TRASHED', 'DELETED')),

    CONSTRAINT chk_conversation_attachments_size
        CHECK (size_bytes >= 0)
);

CREATE INDEX IF NOT EXISTS idx_conversation_attachments_conversation
    ON conversation_attachments(conversation_id, status);

CREATE INDEX IF NOT EXISTS idx_conversation_attachments_storage_object
    ON conversation_attachments(storage_object_id);
