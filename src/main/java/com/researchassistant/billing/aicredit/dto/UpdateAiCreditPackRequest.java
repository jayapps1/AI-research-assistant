package com.researchassistant.billing.aicredit.dto;

import java.math.BigDecimal;

public record UpdateAiCreditPackRequest(
        String name,
        String description,
        BigDecimal creditAmount,
        BigDecimal priceAmount,
        String currency,
        Boolean active,
        Integer displayOrder
) {}
