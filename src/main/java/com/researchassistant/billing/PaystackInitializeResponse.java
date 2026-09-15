package com.researchassistant.billing;

public record PaystackInitializeResponse(
        boolean status,
        String message,
        PaystackInitializeData data
) {
}
