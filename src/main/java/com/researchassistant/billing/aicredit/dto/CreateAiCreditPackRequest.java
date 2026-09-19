package com.researchassistant.billing.aicredit.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateAiCreditPackRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        @NotNull @DecimalMin("0.0001") BigDecimal creditAmount,
        @NotNull @DecimalMin("0.00") BigDecimal priceAmount,
        String currency,
        Boolean active,
        Integer displayOrder
) {}
