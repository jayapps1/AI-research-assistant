package com.researchassistant.billing.aicredit.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AiCreditBalanceResponse(
        IncludedAllowance included,
        PromotionalBalance promotional,
        PurchasedBalance purchased,
        BigDecimal totalAvailable
) {
    public record IncludedAllowance(
            String limitMode,
            Long limit,
            BigDecimal used,
            BigDecimal remaining,
            OffsetDateTime periodStart,
            OffsetDateTime periodEnd
    ) {}

    public record PromotionalBalance(
            BigDecimal remaining
    ) {}

    public record PurchasedBalance(
            BigDecimal remaining
    ) {}
}
