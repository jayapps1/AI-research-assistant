package com.researchassistant.billing;

import com.fasterxml.jackson.databind.JsonNode;

public record PaystackVerificationData(
        String status,
        String reference,
        Long amount,
        String currency,
        JsonNode metadata
) {
}
