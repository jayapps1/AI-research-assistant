package com.researchassistant.billing.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class AdminPaymentDtos {

    private AdminPaymentDtos() {}

    public record AdminPaymentAttemptDetail(
            UUID id,
            int attemptNumber,
            String internalReference,
            String providerReference,
            String status,
            BigDecimal expectedAmount,
            String currency,
            String providerStatus,
            String failureCode,
            String failureMessageSafe,
            String authorizationUrl,
            OffsetDateTime createdAt,
            OffsetDateTime completedAt,
            OffsetDateTime providerVerifiedAt
    ) {}

    public record AdminPaymentIntentResponse(
            UUID id,
            UUID workspaceId,
            String workspaceName,
            UUID userId,
            String userEmail,
            String userName,
            String planCode,
            String planName,
            String billingInterval,
            BigDecimal amount,
            String currency,
            String environment,
            String intentStatus,
            int attemptCount,
            String latestAttemptStatus,
            String latestReference,
            String latestProviderReference,
            String latestProviderStatus,
            String latestFailureCode,
            String latestFailureMessage,
            OffsetDateTime createdAt,
            OffsetDateTime settledAt,
            OffsetDateTime latestVerifiedAt,
            List<AdminPaymentAttemptDetail> attempts
    ) {}
}
