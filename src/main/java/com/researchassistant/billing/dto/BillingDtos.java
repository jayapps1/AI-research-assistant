package com.researchassistant.billing.dto;

import com.researchassistant.billing.*;
import com.researchassistant.subscription.BillingInterval;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class BillingDtos {
    private BillingDtos() {}

    public record InitializePaymentRequest(@NotBlank String planCode, @NotNull BillingInterval billingInterval) {}
    public record InitializePaymentResponse(UUID transactionId, String reference, String authorizationUrl, String accessCode) {}
    public record PaymentAttemptInitializationResponse(UUID paymentIntentId, UUID paymentAttemptId, int attemptNumber,
                                                       String authorizationUrl, String reference, PaymentAttemptStatus status) {}
    public record PaymentAttemptSummary(int attemptNumber, PaymentAttemptStatus status, PaymentFailureCategory failureCategory,
                                        OffsetDateTime createdAt, OffsetDateTime completedAt) {}
    public record PaymentIntentResponse(UUID id, UUID workspaceId, PaymentIntentStatus status, String planCode, BillingInterval billingInterval,
                                        BigDecimal amount, String currency, java.util.List<PaymentAttemptSummary> attempts) {}
    public record PaymentTransactionResponse(UUID id, UUID workspaceId, String reference, PaymentEnvironment environment,
                                             PaymentTransactionStatus status, BigDecimal amount, String currency,
                                             String planCode, BillingInterval billingInterval, OffsetDateTime createdAt) {}
}
