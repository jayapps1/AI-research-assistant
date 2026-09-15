package com.researchassistant.billing;

public enum PaymentTransactionStatus {
    INITIALIZED,
    PENDING,
    SUCCESS,
    FAILED,
    ABANDONED,
    CANCELLED,
    VERIFICATION_REQUIRED,
    VERIFICATION_FAILED
}
