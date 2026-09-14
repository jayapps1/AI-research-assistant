package com.researchassistant.operations.backup;

public enum BackupRunStatus {
    REQUESTED,
    RUNNING,
    COMPLETED,
    FAILED,
    VERIFICATION_PENDING,
    VERIFIED,
    VERIFICATION_FAILED
}
