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
    public record PaymentAttemptDetailResponse(UUID paymentAttemptId, UUID paymentIntentId, UUID workspaceId,
                                               String planCode, String planName, BigDecimal expectedAmount, String currency,
                                               int attemptNumber, PaymentAttemptStatus status, String providerStatus,
                                               String internalReference, String providerReference, String authorizationUrl,
                                               String failureCode, String failureMessageSafe, boolean retryable,
                                               OffsetDateTime createdAt, OffsetDateTime completedAt, OffsetDateTime providerVerifiedAt) {}
    public record PlanPriceBreakdownResponse(String planCode, BillingInterval billingInterval, String currency,
                                            BigDecimal baseAmount, BigDecimal processingRate, BigDecimal processingAmount,
                                            BigDecimal aiGenerationRate, BigDecimal aiGenerationAmount, BigDecimal totalAmount) {}
}
