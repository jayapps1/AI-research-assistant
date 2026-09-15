package com.researchassistant.billing;

public record PaystackVerificationResponse(
        boolean status,
        String message,
        PaystackVerificationData data
) {
}
