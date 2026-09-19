package com.researchassistant.billing.aicredit.exception;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class AiCreditsExhaustedException extends RuntimeException {

    private final UUID workspaceId;
    private final BigDecimal includedRemaining;
    private final BigDecimal promotionalRemaining;
    private final BigDecimal purchasedRemaining;
    private final BigDecimal totalAvailable;
    private final boolean canPurchaseCredits;

    public AiCreditsExhaustedException(
            UUID workspaceId,
            BigDecimal includedRemaining,
            BigDecimal promotionalRemaining,
            BigDecimal purchasedRemaining,
            BigDecimal totalAvailable,
            boolean canPurchaseCredits
    ) {
        super("AI credits exhausted for workspace " + workspaceId + ". Please top up or purchase AI credits.");
        this.workspaceId = workspaceId;
        this.includedRemaining = includedRemaining != null ? includedRemaining : BigDecimal.ZERO;
        this.promotionalRemaining = promotionalRemaining != null ? promotionalRemaining : BigDecimal.ZERO;
        this.purchasedRemaining = purchasedRemaining != null ? purchasedRemaining : BigDecimal.ZERO;
        this.totalAvailable = totalAvailable != null ? totalAvailable : BigDecimal.ZERO;
        this.canPurchaseCredits = canPurchaseCredits;
    }
}
