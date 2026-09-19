package com.researchassistant.billing.aicredit.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AiCreditPackResponse(
        UUID id,
        String code,
        String name,
        String description,
        BigDecimal credits,
        BigDecimal price,
        String currency,
        boolean active,
        int displayOrder,
        OffsetDateTime createdAt
) {}
