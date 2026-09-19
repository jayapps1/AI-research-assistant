package com.researchassistant.billing.aicredit.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AiCreditLedgerItemResponse(
        UUID id,
        String bucket,
        String type,
        BigDecimal creditAmount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String sourceType,
        UUID sourceId,
        UUID aiRequestId,
        UUID paymentIntentId,
        UUID paymentAttemptId,
        UUID purchaseId,
        String createdByName,
        OffsetDateTime createdAt
) {}
