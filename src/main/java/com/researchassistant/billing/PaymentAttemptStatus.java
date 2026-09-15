package com.researchassistant.billing;

public enum PaymentAttemptStatus {
    CREATED,
    INITIALIZED,
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    ABANDONED,
    CANCELLED,
    EXPIRED,
    VERIFICATION_FAILED
}
