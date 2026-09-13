-- ============================================================
-- AI RESEARCH ASSISTANT
-- V8 - DOCUMENTS, VERSIONS AND PROCESSING JOBS
-- ============================================================
--
-- Purpose:
-- Adds logical research documents, immutable uploaded file versions
-- and queued processing jobs. This migration deliberately stores only
-- metadata in PostgreSQL; binary files live behind the application
-- storage abstraction.
--
-- Numbering:
-- document_number/document_code are permanent within a project.
-- next_version_number is permanent within a document. Neither may be
-- derived from row counts or reused after archive/failure.
-- ============================================================

CREATE TABLE documents
(
    id UUID PRIMARY KEY,

    project_id UUID NOT NULL,

    document_number BIGINT NOT NULL,

    document_code VARCHAR(32) NOT NULL,

    title VARCHAR(500) NOT NULL,

    type VARCHAR(40) NOT NULL,

    status VARCHAR(40) NOT NULL,

    created_by UUID NOT NULL,

    next_version_number INTEGER
        NOT NULL
        DEFAULT 1,

    current_version_id UUID,

    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    archived_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_documents_project
        FOREIGN KEY (project_id)
        REFERENCES research_projects(id),

    CONSTRAINT fk_documents_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT uk_documents_project_number
        UNIQUE (project_id, document_number),

    CONSTRAINT uk_documents_project_code
        UNIQUE (project_id, document_code),

    CONSTRAINT chk_documents_number
        CHECK (document_number >= 1),

    CONSTRAINT chk_documents_next_version_number
        CHECK (next_version_number >= 1),

    CONSTRAINT chk_documents_type
        CHECK (
            type IN (
                'PDF',
                'WORD',
                'TEXT',
                'SPREADSHEET',
                'PRESENTATION',
                'IMAGE',
                'OTHER'
            )
        ),

    CONSTRAINT chk_documents_status
        CHECK (
            status IN (
                'UPLOADING',
                'PROCESSING',
                'READY',
                'FAILED',
                'ARCHIVED'
            )
        )
);

CREATE INDEX idx_documents_project_id
    ON documents(project_id);

CREATE INDEX idx_documents_project_status
    ON documents(project_id, status);

CREATE INDEX idx_documents_created_by
    ON documents(created_by);

CREATE INDEX idx_documents_document_code
    ON documents(document_code);

CREATE TABLE document_versions
(
    id UUID PRIMARY KEY,

    document_id UUID NOT NULL,

    version_number INTEGER NOT NULL,

    original_filename VARCHAR(500) NOT NULL,

    storage_key VARCHAR(1000) NOT NULL,

    mime_type VARCHAR(255) NOT NULL,

    file_size_bytes BIGINT NOT NULL,

    checksum_sha256 VARCHAR(64) NOT NULL,

    status VARCHAR(40) NOT NULL,

    uploaded_by UUID NOT NULL,

    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_document_versions_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id),

    CONSTRAINT fk_document_versions_uploaded_by
        FOREIGN KEY (uploaded_by)
        REFERENCES users(id),

    CONSTRAINT uk_document_versions_document_number
        UNIQUE (document_id, version_number),

    CONSTRAINT chk_document_versions_version_number
        CHECK (version_number >= 1),

    CONSTRAINT chk_document_versions_file_size
        CHECK (file_size_bytes >= 0),

    CONSTRAINT chk_document_versions_status
        CHECK (
            status IN (
                'UPLOADING',
                'STORED',
                'PROCESSING',
                'READY',
                'FAILED',
                'SUPERSEDED'
            )
        )
);

ALTER TABLE documents
    ADD CONSTRAINT fk_documents_current_version
        FOREIGN KEY (current_version_id)
        REFERENCES document_versions(id);

CREATE INDEX idx_document_versions_document_id
    ON document_versions(document_id);

CREATE INDEX idx_document_versions_document_status
    ON document_versions(document_id, status);

CREATE INDEX idx_document_versions_uploaded_by
    ON document_versions(uploaded_by);

CREATE INDEX idx_document_versions_checksum
    ON document_versions(checksum_sha256);

CREATE TABLE document_processing_jobs
(
    id UUID PRIMARY KEY,

    document_version_id UUID NOT NULL,

    type VARCHAR(40) NOT NULL,

    status VARCHAR(40) NOT NULL,

    attempt_number INTEGER NOT NULL,

    error_code VARCHAR(100),

    error_message VARCHAR(2000),

    queued_at TIMESTAMP WITH TIME ZONE NOT NULL,

    started_at TIMESTAMP WITH TIME ZONE,

    completed_at TIMESTAMP WITH TIME ZONE,

    failed_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_document_processing_jobs_version
        FOREIGN KEY (document_version_id)
        REFERENCES document_versions(id),

    CONSTRAINT chk_document_processing_jobs_attempt
        CHECK (attempt_number >= 1),

    CONSTRAINT chk_document_processing_jobs_type
        CHECK (
            type IN (
                'INGESTION'
            )
        ),

    CONSTRAINT chk_document_processing_jobs_status
        CHECK (
            status IN (
                'QUEUED',
                'RUNNING',
                'COMPLETED',
                'FAILED',
                'CANCELLED'
            )
        )
);

CREATE INDEX idx_document_processing_jobs_version_id
    ON document_processing_jobs(document_version_id);

CREATE INDEX idx_document_processing_jobs_status
    ON document_processing_jobs(status);

CREATE INDEX idx_document_processing_jobs_type_status
    ON document_processing_jobs(type, status);

CREATE INDEX idx_document_processing_jobs_queued_at
    ON document_processing_jobs(queued_at);
