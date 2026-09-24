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
    private final BigDecimal estimatedRequiredCredits;
    private final boolean canPurchaseCredits;

    public AiCreditsExhaustedException(
            UUID workspaceId,
            BigDecimal includedRemaining,
            BigDecimal promotionalRemaining,
            BigDecimal purchasedRemaining,
            BigDecimal totalAvailable,
            BigDecimal estimatedRequiredCredits,
            boolean canPurchaseCredits
    ) {
        super("You do not have enough AI credits for this request.");
        this.workspaceId = workspaceId;
        this.includedRemaining = includedRemaining != null ? includedRemaining : BigDecimal.ZERO;
        this.promotionalRemaining = promotionalRemaining != null ? promotionalRemaining : BigDecimal.ZERO;
        this.purchasedRemaining = purchasedRemaining != null ? purchasedRemaining : BigDecimal.ZERO;
        this.totalAvailable = totalAvailable != null ? totalAvailable : BigDecimal.ZERO;
        this.estimatedRequiredCredits = estimatedRequiredCredits != null ? estimatedRequiredCredits : BigDecimal.ZERO;
        this.canPurchaseCredits = canPurchaseCredits;
    }
}
