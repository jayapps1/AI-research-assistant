package com.researchassistant.ai.usage;

public enum AiRequestStatus {
    RECEIVED,
    RUNNING,
    COMPLETED,
    FAILED,
    TIMED_OUT,
    REJECTED_BY_POLICY,
    CAPABILITY_UNAVAILABLE,
    CANCELLED
}
