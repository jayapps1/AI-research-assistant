package com.researchassistant.billing.aicredit.service;

import java.math.BigDecimal;
import java.util.UUID;

public record AiCreditReservation(
        UUID workspaceId,
        BigDecimal estimatedCredits,
        BigDecimal reservedFromIncluded,
        BigDecimal reservedFromWallet
) {}
