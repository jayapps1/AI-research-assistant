package com.researchassistant.billing.aicredit.dto;

import jakarta.validation.constraints.NotBlank;

public record BuyAiCreditPackRequest(
        @NotBlank String creditPackCode
) {}
