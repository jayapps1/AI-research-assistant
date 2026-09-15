package com.researchassistant.billing;

public enum PaymentFailureCategory {
    PAYMENT_DECLINED,
    PAYMENT_CANCELLED,
    PAYMENT_EXPIRED,
    PROVIDER_UNAVAILABLE,
    VERIFICATION_FAILED,
    UNKNOWN
}
