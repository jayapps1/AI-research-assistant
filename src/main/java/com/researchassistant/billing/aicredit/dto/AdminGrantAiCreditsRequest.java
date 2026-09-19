package com.researchassistant.billing.aicredit.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AdminGrantAiCreditsRequest(
        @NotNull @DecimalMin("0.0001") BigDecimal creditAmount,
        @NotBlank String bucket,
        @NotBlank String reason
) {}
