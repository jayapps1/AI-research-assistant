CREATE TABLE backup_policies (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL CHECK (status IN ('ACTIVE','DISABLED')),
    frequency VARCHAR(30) NOT NULL CHECK (frequency IN ('DAILY','WEEKLY','MONTHLY','CUSTOM')),
    retention_daily INTEGER NOT NULL CHECK (retention_daily >= 0),
    retention_weekly INTEGER NOT NULL CHECK (retention_weekly >= 0),
    retention_monthly INTEGER NOT NULL CHECK (retention_monthly >= 0),
    database_backup_enabled BOOLEAN NOT NULL,
    object_storage_backup_enabled BOOLEAN NOT NULL,
    verification_required BOOLEAN NOT NULL,
    target_rpo_minutes INTEGER CHECK (target_rpo_minutes IS NULL OR target_rpo_minutes > 0),
    target_rto_minutes INTEGER CHECK (target_rto_minutes IS NULL OR target_rto_minutes > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE backup_runs (
    id UUID PRIMARY KEY,
    policy_id UUID REFERENCES backup_policies(id),
    type VARCHAR(40) NOT NULL CHECK (type IN ('FULL','DATABASE_ONLY','OBJECT_STORAGE_ONLY','VERIFICATION')),
    status VARCHAR(40) NOT NULL CHECK (status IN ('REQUESTED','RUNNING','COMPLETED','FAILED','VERIFICATION_PENDING','VERIFIED','VERIFICATION_FAILED')),
    initiated_by VARCHAR(255) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    application_version VARCHAR(255),
    database_schema_version VARCHAR(100),
    database_backup_reference TEXT,
    object_storage_backup_reference TEXT,
    manifest_storage_reference TEXT,
    error_code VARCHAR(100),
    error_message TEXT,
    duration_ms BIGINT CHECK (duration_ms IS NULL OR duration_ms >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (completed_at IS NULL OR completed_at >= started_at)
);
CREATE INDEX idx_backup_runs_status ON backup_runs(status);
CREATE INDEX idx_backup_runs_started_at ON backup_runs(started_at);
CREATE INDEX idx_backup_runs_completed_at ON backup_runs(completed_at);

CREATE TABLE backup_artifacts (
    id UUID PRIMARY KEY,
    backup_run_id UUID NOT NULL REFERENCES backup_runs(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL CHECK (type IN ('POSTGRESQL_DUMP','OBJECT_STORAGE_ARCHIVE','OBJECT_STORAGE_MANIFEST','BACKUP_MANIFEST','WAL_REFERENCE')),
    storage_reference TEXT NOT NULL,
    size_bytes BIGINT CHECK (size_bytes IS NULL OR size_bytes >= 0),
    sha256_checksum VARCHAR(64),
    status VARCHAR(40) NOT NULL CHECK (status IN ('AVAILABLE','MISSING','CORRUPT','RETENTION_ELIGIBLE','RETAINED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_backup_artifacts_run ON backup_artifacts(backup_run_id);
CREATE INDEX idx_backup_artifacts_type ON backup_artifacts(type);

CREATE TABLE restore_runs (
    id UUID PRIMARY KEY,
    backup_run_id UUID NOT NULL REFERENCES backup_runs(id),
    status VARCHAR(40) NOT NULL CHECK (status IN ('REQUESTED','VALIDATING_BACKUP','RESTORING_DATABASE','RESTORING_OBJECT_STORAGE','VERIFYING','COMPLETED','FAILED')),
    target_environment VARCHAR(40) NOT NULL CHECK (target_environment IN ('TEST','STAGING','DISASTER_RECOVERY','PRODUCTION')),
    target_database_reference TEXT,
    target_storage_reference TEXT,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    initiated_by VARCHAR(255) NOT NULL,
    error_code VARCHAR(100),
    error_message TEXT,
    duration_ms BIGINT CHECK (duration_ms IS NULL OR duration_ms >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (completed_at IS NULL OR completed_at >= started_at)
);
CREATE INDEX idx_restore_runs_status ON restore_runs(status);
CREATE INDEX idx_restore_runs_started_at ON restore_runs(started_at);
CREATE INDEX idx_restore_runs_backup_run ON restore_runs(backup_run_id);
