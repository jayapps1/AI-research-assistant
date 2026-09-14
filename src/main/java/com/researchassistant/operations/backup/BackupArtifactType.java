package com.researchassistant.operations.backup;

public enum BackupArtifactType {
    POSTGRESQL_DUMP,
    OBJECT_STORAGE_ARCHIVE,
    OBJECT_STORAGE_MANIFEST,
    BACKUP_MANIFEST,
    WAL_REFERENCE
}
