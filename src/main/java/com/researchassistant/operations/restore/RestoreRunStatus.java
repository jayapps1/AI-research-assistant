package com.researchassistant.operations.restore;

public enum RestoreRunStatus {
    REQUESTED,
    VALIDATING_BACKUP,
    RESTORING_DATABASE,
    RESTORING_OBJECT_STORAGE,
    VERIFYING,
    COMPLETED,
    FAILED
}
