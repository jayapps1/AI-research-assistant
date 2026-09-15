package com.researchassistant.jobs;

public enum BackgroundJobStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    RETRY_SCHEDULED,
    CANCELLED,
    DEAD_LETTER
}
