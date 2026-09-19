package com.researchassistant.billing;

import com.researchassistant.audit.AuditEventService;
import com.researchassistant.audit.AuditEventType;
import com.researchassistant.billing.dto.BillingDtos;
import com.researchassistant.billing.dto.BillingDtos.InitializePaymentResponse;
import com.researchassistant.billing.dto.BillingDtos.PaymentAttemptDetailResponse;
import com.researchassistant.billing.dto.BillingDtos.PaymentAttemptInitializationResponse;
import com.researchassistant.billing.dto.BillingDtos.PaymentAttemptSummary;
import com.researchassistant.billing.dto.BillingDtos.PaymentIntentResponse;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.notification.NotificationPriority;
import com.researchassistant.notification.NotificationService;
import com.researchassistant.notification.NotificationType;
import com.researchassistant.subscription.*;
import com.researchassistant.workspace.entity.Workspace;
import com.researchassistant.workspace.repository.WorkspaceRepository;
import com.researchassistant.billing.aicredit.service.AiCreditPurchaseService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Transactional
public class BillingService {
    private final WorkspaceRepository workspaceRepository;
    private final SubscriptionPlanRepository planRepository;
    private final WorkspaceSubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentProvider paymentProvider;
    private final SubscriptionChangeService subscriptionChangeService;
    private final AuditEventService auditEventService;
    private final BillingPaymentIntentRepository intentRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final PaymentIdempotencyRecordRepository idempotencyRecordRepository;
    private final PaymentProperties paymentProperties;
    private final NotificationService notificationService;
    private final FreeSubscriptionProvisioningService freeSubscriptionProvisioningService;
    private final SubscriptionPlanPriceRepository planPriceRepository;
    private final MoneyMinorUnitConverter moneyMinorUnitConverter;
    private final SecureRandom random = new SecureRandom();

    private final BillingPriceCalculator priceCalculator;
    private final ObjectProvider<AiCreditPurchaseService> creditPurchaseServiceProvider;

    public BillingService(WorkspaceRepository workspaceRepository, SubscriptionPlanRepository planRepository,
                          WorkspaceSubscriptionRepository subscriptionRepository, PaymentTransactionRepository transactionRepository,
                          PaymentProvider paymentProvider, SubscriptionChangeService subscriptionChangeService,
                          AuditEventService auditEventService, BillingPaymentIntentRepository intentRepository,
                          PaymentAttemptRepository attemptRepository, PaymentIdempotencyRecordRepository idempotencyRecordRepository,
                          PaymentProperties paymentProperties, NotificationService notificationService,
                          FreeSubscriptionProvisioningService freeSubscriptionProvisioningService,
                          SubscriptionPlanPriceRepository planPriceRepository,
                          MoneyMinorUnitConverter moneyMinorUnitConverter,
                          BillingPriceCalculator priceCalculator,
                          ObjectProvider<AiCreditPurchaseService> creditPurchaseServiceProvider) {
        this.workspaceRepository = workspaceRepository;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.transactionRepository = transactionRepository;
        this.paymentProvider = paymentProvider;
        this.subscriptionChangeService = subscriptionChangeService;
        this.auditEventService = auditEventService;
        this.intentRepository = intentRepository;
        this.attemptRepository = attemptRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.paymentProperties = paymentProperties;
        this.notificationService = notificationService;
        this.freeSubscriptionProvisioningService = freeSubscriptionProvisioningService;
        this.planPriceRepository = planPriceRepository;
        this.moneyMinorUnitConverter = moneyMinorUnitConverter;
        this.priceCalculator = priceCalculator;
        this.creditPurchaseServiceProvider = creditPurchaseServiceProvider;
    }

    public InitializePaymentResponse initialize(UUID workspaceId, User user, String planCode, BillingInterval interval) {
        PaymentAttemptInitializationResponse response = initializeIntent(workspaceId, user, planCode, interval, null);
        return new InitializePaymentResponse(response.paymentIntentId(), response.reference(), response.authorizationUrl(), null);
    }

    public PaymentAttemptInitializationResponse initializeIntent(UUID workspaceId, User user, String planCode, BillingInterval interval, String idempotencyKey) {
        PaymentAttemptInitializationResponse idempotent = existingIdempotentResponse("INITIALIZE", idempotencyKey);
        if (idempotent != null) return idempotent;
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found."));
        SubscriptionPlan plan = planRepository.findByCodeIgnoreCase(planCode)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found."));
        if (plan.getStatus() != SubscriptionPlanStatus.ACTIVE || !plan.isPubliclyAvailable()) {
            throw new IllegalArgumentException("Subscription plan '" + plan.getCode() + "' is not available for purchase.");
        }

        // Authoritative price resolution from SubscriptionPlanPrice
        SubscriptionPlanPrice planPrice = planPriceRepository
                .findByPlanCodeIgnoreCaseAndBillingIntervalAndActiveTrue(plan.getCode(), interval)
                .orElse(null);

        if (interval == BillingInterval.YEARLY && planPrice == null) {
            throw new IllegalArgumentException("Yearly billing is not configured for plan '" + plan.getCode() + "'.");
        }

        java.math.BigDecimal authoritativeBaseAmount;
        String authoritativeCurrency;
        if (planPrice != null) {
            authoritativeBaseAmount = planPrice.getPrice();
            authoritativeCurrency = planPrice.getCurrency();
        } else if (interval == plan.getBillingInterval() || interval == BillingInterval.MONTHLY) {
            authoritativeBaseAmount = plan.getPrice();
            authoritativeCurrency = plan.getCurrency();
        } else {
            throw new IllegalArgumentException("Billing interval '" + interval + "' is not available for plan '" + plan.getCode() + "'.");
        }

        BillingDtos.PlanPriceBreakdownResponse breakdown = priceCalculator.calculate(plan.getCode(), interval, authoritativeBaseAmount, authoritativeCurrency);

        if (breakdown.totalAmount().signum() <= 0 || interval == BillingInterval.NONE) {
            WorkspaceSubscription subscription = subscriptionChangeService.activatePlan(workspaceId, plan.getCode(), BillingInterval.NONE, null, user.getId(), "FREE_ACTIVATED", SubscriptionAccessSource.FREE_DEFAULT);
            return new PaymentAttemptInitializationResponse(subscription.getId(), null, 0, null, "FREE", PaymentAttemptStatus.SUCCESS);
        }
        BillingPaymentIntent intent = new BillingPaymentIntent();
        intent.setWorkspace(workspace);
        intent.setInitiatedBy(user);
        intent.setPlan(plan);
        intent.setBillingInterval(interval);
        intent.setBaseAmount(breakdown.baseAmount());
        intent.setProcessingFeeAmount(breakdown.processingAmount());
        intent.setAiGenerationFeeAmount(breakdown.aiGenerationAmount());
        intent.setTotalAmount(breakdown.totalAmount());
        intent.setExpectedAmount(breakdown.totalAmount());
        intent.setCurrency(breakdown.currency());
        intent.setStatus(PaymentIntentStatus.OPEN);
        intent.setExpiresAt(OffsetDateTime.now().plus(paymentProperties.intentTtl()));
        intent = intentRepository.saveAndFlush(intent);
        auditEventService.record(user.getId(), "USER", workspace, null, AuditEventType.PAYMENT_INTENT_CREATED,
                "BillingPaymentIntent", intent.getId(), "{\"plan\":\"" + plan.getCode() + "\",\"base\":" + breakdown.baseAmount() + ",\"total\":" + breakdown.totalAmount() + ",\"interval\":\"" + interval + "\"}");
        PaymentAttemptInitializationResponse response = initializeAttempt(intent, user, false);
        saveIdempotent("INITIALIZE", idempotencyKey, user, response);
        return response;
    }

    @Transactional(readOnly = true)
    public BillingDtos.PlanPriceBreakdownResponse getPriceBreakdown(String planCode, BillingInterval interval) {
        SubscriptionPlan plan = planRepository.findByCodeIgnoreCase(planCode)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found: " + planCode));
        SubscriptionPlanPrice planPrice = planPriceRepository
                .findByPlanCodeIgnoreCaseAndBillingIntervalAndActiveTrue(plan.getCode(), interval)
                .orElse(null);
        if (interval == BillingInterval.YEARLY && planPrice == null) {
            throw new IllegalArgumentException("Yearly billing is not configured for plan '" + plan.getCode() + "'.");
        }
        java.math.BigDecimal base = planPrice != null ? planPrice.getPrice() : plan.getPrice();
        String curr = planPrice != null ? planPrice.getCurrency() : plan.getCurrency();
        return priceCalculator.calculate(plan.getCode(), interval, base, curr);
    }

    public PaymentAttemptInitializationResponse retry(UUID paymentIntentId, User user, String idempotencyKey) {
        PaymentAttemptInitializationResponse idempotent = existingIdempotentResponse("RETRY:" + paymentIntentId, idempotencyKey);
        if (idempotent != null) return idempotent;
        BillingPaymentIntent intent = intentRepository.findByIdForUpdate(paymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment intent not found."));
        if (!intent.getInitiatedBy().getId().equals(user.getId())) {
            throw new IllegalStateException("Only the initiating user may retry this payment.");
        }
        if (intent.getStatus() == PaymentIntentStatus.PAID) throw new IllegalStateException("Payment intent is already paid.");
        if (intent.getExpiresAt() != null && !intent.getExpiresAt().isAfter(OffsetDateTime.now())) {
            intent.setStatus(PaymentIntentStatus.EXPIRED);
            throw new IllegalStateException("Payment intent has expired. Create a new payment intent for current pricing.");
        }
        PaymentAttempt latest = attemptRepository.findFirstByPaymentIntentIdOrderByAttemptNumberDesc(paymentIntentId)
                .orElseThrow(() -> new IllegalStateException("Payment intent has no attempt to retry."));
        if (!isRetryable(latest)) throw new IllegalStateException("Latest payment attempt is still active or not retryable.");
        if (latest.getCompletedAt() != null && latest.getCompletedAt().plus(paymentProperties.retryCooldown()).isAfter(OffsetDateTime.now())) {
            throw new IllegalStateException("Retry cooldown is still active.");
        }
        if (attemptRepository.countByPaymentIntentId(paymentIntentId) >= paymentProperties.maxAttemptsPerIntent()) {
            throw new IllegalStateException("Maximum payment attempts reached for this intent.");
        }
        PaymentAttemptInitializationResponse response = initializeAttempt(intent, user, true);
        saveIdempotent("RETRY:" + paymentIntentId, idempotencyKey, user, response);
        return response;
    }

    public PaymentTransaction verify(UUID transactionId, User actor) {
        return transactionRepository.findById(transactionId)
                .map(transaction -> verifyLegacyReference(transaction.getInternalReference(), actor == null ? null : actor.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found."));
    }

    public PaymentTransaction verifyReference(String reference, UUID actorId) {
        return verifyLegacyReference(reference, actorId);
    }

    public PaymentAttempt verifyAttemptReference(String reference, UUID actorId) {
        PaymentAttempt attempt = attemptRepository.findByInternalReference(reference)
                .or(() -> attemptRepository.findByProviderAndEnvironmentAndProviderReference(PaymentProviderType.PAYSTACK, paymentProvider.environment(), reference))
                .orElseThrow(() -> new ResourceNotFoundException("Payment attempt not found."));
        return verifyAttempt(attempt.getId(), actorId);
    }

    public PaymentAttempt verifyAttempt(UUID attemptId, UUID actorId) {
        PaymentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment attempt not found."));
        if (attempt.getStatus() == PaymentAttemptStatus.SUCCESS && attempt.getProviderVerifiedAt() != null) return attempt;
        PaystackVerificationResponse response = paymentProvider.verify(attempt.getInternalReference());
        PaystackVerificationData data = response == null ? null : response.data();
        attempt.setProviderVerifiedAt(OffsetDateTime.now());
        if (data == null) {
            attempt.setStatus(PaymentAttemptStatus.VERIFICATION_FAILED);
            attempt.setFailureCode("VERIFICATION_FAILED");
            attempt.setFailureMessageSafe(PaymentFailureCategory.PROVIDER_UNAVAILABLE.name());
            auditEventService.record(actorId, "USER", attempt.getPaymentIntent().getWorkspace(), null, AuditEventType.PAYMENT_ATTEMPT_FAILED,
                    "PaymentAttempt", attempt.getId(), "{}");
            return attempt;
        }
        attempt.setProviderStatus(data.status());
        if (!"success".equalsIgnoreCase(data.status())) {
            if ("pending".equalsIgnoreCase(data.status()) || "ongoing".equalsIgnoreCase(data.status()) || "processing".equalsIgnoreCase(data.status())) {
                attempt.setStatus(PaymentAttemptStatus.PENDING);
                attempt.getPaymentIntent().setStatus(PaymentIntentStatus.PAYMENT_PENDING);
            } else {
                attempt.setStatus(PaymentAttemptStatus.FAILED);
                attempt.setCompletedAt(OffsetDateTime.now());
                attempt.setFailureCode("PAYMENT_NOT_SUCCESSFUL");
                attempt.setFailureMessageSafe(PaymentFailureCategory.PAYMENT_DECLINED.name());
                notifyPayment(attempt, NotificationType.PAYMENT_RETRY_AVAILABLE, "Payment retry available", "Your payment was not completed. You may try again.");
            }
            return attempt;
        }
        long expected = moneyMinorUnitConverter.toMinorUnits(attempt.getExpectedAmount(), attempt.getCurrency());
        if (!attempt.getInternalReference().equals(data.reference())
                || data.amount() == null || data.amount() != expected
                || !attempt.getCurrency().equalsIgnoreCase(data.currency())
                || !attemptMetadataMatches(attempt, data)) {
            attempt.setStatus(PaymentAttemptStatus.VERIFICATION_FAILED);
            attempt.setCompletedAt(OffsetDateTime.now());
            attempt.setFailureCode("VERIFICATION_MISMATCH");
            attempt.setFailureMessageSafe(PaymentFailureCategory.VERIFICATION_FAILED.name());
            auditEventService.record(actorId, "USER", attempt.getPaymentIntent().getWorkspace(), null, AuditEventType.PAYMENT_ATTEMPT_FAILED,
                    "PaymentAttempt", attempt.getId(), "{\"reason\":\"VERIFICATION_MISMATCH\",\"expectedMinorUnits\":" + expected + ",\"receivedMinorUnits\":" + data.amount() + "}");
            return attempt;
        }
        settleSuccessfulAttempt(attempt, actorId);
        return attempt;
    }

    @Transactional(readOnly = true)
    public PaymentAttempt getAttempt(UUID attemptId) {
        return attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment attempt not found."));
    }

    @Transactional
    public BillingDtos.PaymentAttemptDetailResponse getOrVerifyAttemptByReference(String reference, UUID actorId) {
        PaymentAttempt attempt = attemptRepository.findByInternalReference(reference)
                .or(() -> attemptRepository.findByProviderAndEnvironmentAndProviderReference(PaymentProviderType.PAYSTACK, paymentProvider.environment(), reference))
                .orElseThrow(() -> new ResourceNotFoundException("Payment attempt not found with reference: " + reference));

        if (attempt.getStatus() == PaymentAttemptStatus.PENDING || attempt.getStatus() == PaymentAttemptStatus.CREATED) {
            try {
                attempt = verifyAttempt(attempt.getId(), actorId);
            } catch (Exception ignored) {
            }
        }
        return toDetailResponse(attempt);
    }

    @Transactional(readOnly = true)
    public BillingDtos.PaymentAttemptDetailResponse getAttemptDetail(UUID attemptId) {
        PaymentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment attempt not found: " + attemptId));
        return toDetailResponse(attempt);
    }

    private BillingDtos.PaymentAttemptDetailResponse toDetailResponse(PaymentAttempt attempt) {
        BillingPaymentIntent intent = attempt.getPaymentIntent();
        String planCode = intent.getPlan() != null ? intent.getPlan().getCode()
                : (intent.getAiCreditPurchase() != null ? intent.getAiCreditPurchase().getPackCode() : "AI_CREDIT_PACK");
        String planName = intent.getPlan() != null ? intent.getPlan().getName()
                : (intent.getAiCreditPurchase() != null ? intent.getAiCreditPurchase().getPackName() : "AI Credit Pack");
        return new BillingDtos.PaymentAttemptDetailResponse(
                attempt.getId(),
                intent.getId(),
                intent.getWorkspace().getId(),
                planCode,
                planName,
                attempt.getExpectedAmount(),
                attempt.getCurrency(),
                attempt.getAttemptNumber(),
                attempt.getStatus(),
                attempt.getProviderStatus(),
                attempt.getInternalReference(),
                attempt.getProviderReference(),
                attempt.getAuthorizationUrl(),
                attempt.getFailureCode(),
                attempt.getFailureMessageSafe(),
                isRetryable(attempt),
                attempt.getCreatedAt(),
                attempt.getCompletedAt(),
                attempt.getProviderVerifiedAt()
        );
    }

    private PaymentTransaction verifyLegacyReference(String reference, UUID actorId) {
        PaymentTransaction transaction = transactionRepository.findByInternalReference(reference)
                .orElseGet(() -> transactionRepository.findByProviderAndEnvironmentAndProviderReference(PaymentProviderType.PAYSTACK, paymentProvider.environment(), reference)
                        .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found.")));
        if (transaction.getStatus() == PaymentTransactionStatus.SUCCESS) {
            return transaction;
        }
        PaystackVerificationResponse response = paymentProvider.verify(transaction.getInternalReference());
        PaystackVerificationData data = response == null ? null : response.data();
        long expected = moneyMinorUnitConverter.toMinorUnits(transaction.getAmount(), transaction.getCurrency());
        if (data == null || !"success".equalsIgnoreCase(data.status())
                || !transaction.getInternalReference().equals(data.reference())
                || data.amount() == null || data.amount() != expected
                || !transaction.getCurrency().equalsIgnoreCase(data.currency())
                || !metadataMatches(transaction, data)) {
            transaction.setStatus(PaymentTransactionStatus.VERIFICATION_FAILED);
            transaction.setFailureCode("VERIFICATION_MISMATCH");
            auditEventService.record(actorId, "USER", transaction.getWorkspace(), null, AuditEventType.PAYMENT_FAILED,
                    "PaymentTransaction", transaction.getId(), "{\"reason\":\"VERIFICATION_MISMATCH\",\"expectedMinorUnits\":" + expected + ",\"receivedMinorUnits\":" + (data == null ? null : data.amount()) + "}");
            return transaction;
        }
        OffsetDateTime now = OffsetDateTime.now();
        transaction.setStatus(PaymentTransactionStatus.SUCCESS);
        transaction.setVerifiedAt(now);
        transaction.setCompletedAt(now);
        subscriptionChangeService.activatePlan(transaction.getWorkspace().getId(), transaction.getPlanCode(),
                transaction.getBillingInterval(), transaction.getProviderReference(), actorId, "PAYMENT_SUCCESS", SubscriptionAccessSource.PAID);
        auditEventService.record(actorId, "USER", transaction.getWorkspace(), null, AuditEventType.PAYMENT_SUCCESS,
                "PaymentTransaction", transaction.getId(), "{\"environment\":\"TEST\"}");
        return transaction;
    }

    @Transactional(readOnly = true)
    public PaymentIntentResponse getIntent(UUID intentId) {
        BillingPaymentIntent intent = intentRepository.findById(intentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment intent not found."));
        List<PaymentAttemptSummary> attempts = attemptRepository.findAllByPaymentIntentIdOrderByAttemptNumberAsc(intentId)
                .stream()
                .map(a -> new PaymentAttemptSummary(a.getAttemptNumber(), a.getStatus(), safeFailure(a), a.getCreatedAt(), a.getCompletedAt()))
                .toList();
        String planCode = intent.getPlan() != null ? intent.getPlan().getCode()
                : (intent.getAiCreditPurchase() != null ? intent.getAiCreditPurchase().getPackCode() : "AI_CREDIT_PACK");
        return new PaymentIntentResponse(intent.getId(), intent.getWorkspace().getId(), intent.getStatus(), planCode, intent.getBillingInterval(),
                intent.getExpectedAmount(), intent.getCurrency(), attempts);
    }

    private boolean metadataMatches(PaymentTransaction transaction, PaystackVerificationData data) {
        if (data.metadata() == null || data.metadata().isNull() || data.metadata().isMissingNode()) {
            return false;
        }
        return transaction.getId().toString().equals(data.metadata().path("internalPaymentId").asText())
                && transaction.getWorkspace().getId().toString().equals(data.metadata().path("workspaceId").asText())
                && transaction.getPlanCode().equalsIgnoreCase(data.metadata().path("planCode").asText())
                && transaction.getBillingInterval().name().equalsIgnoreCase(data.metadata().path("billingInterval").asText());
    }

    private boolean attemptMetadataMatches(PaymentAttempt attempt, PaystackVerificationData data) {
        if (data.metadata() == null || data.metadata().isNull() || data.metadata().isMissingNode()) return false;
        BillingPaymentIntent intent = attempt.getPaymentIntent();
        if (intent.getPurchaseType() == BillingPurchaseType.AI_CREDIT_PACK) {
            boolean baseMatch = intent.getId().toString().equals(data.metadata().path("internalPaymentIntentId").asText())
                    && attempt.getId().toString().equals(data.metadata().path("internalPaymentAttemptId").asText())
                    && intent.getWorkspace().getId().toString().equals(data.metadata().path("workspaceId").asText());
            if (!baseMatch) return false;
            String packCode = data.metadata().path("packCode").asText();
            if (packCode != null && !packCode.isBlank() && intent.getAiCreditPurchase() != null) {
                return intent.getAiCreditPurchase().getPackCode().equalsIgnoreCase(packCode);
            }
            return true;
        }
        return intent.getId().toString().equals(data.metadata().path("internalPaymentIntentId").asText())
                && attempt.getId().toString().equals(data.metadata().path("internalPaymentAttemptId").asText())
                && intent.getWorkspace().getId().toString().equals(data.metadata().path("workspaceId").asText())
                && (intent.getPlan() == null || intent.getPlan().getCode().equalsIgnoreCase(data.metadata().path("planCode").asText()))
                && (intent.getBillingInterval() == null || intent.getBillingInterval().name().equalsIgnoreCase(data.metadata().path("billingInterval").asText()));
    }

    private PaymentAttemptInitializationResponse initializeAttempt(BillingPaymentIntent intent, User user, boolean retry) {
        int attemptNumber = intent.getNextAttemptNumber();
        intent.setNextAttemptNumber(attemptNumber + 1);
        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setPaymentIntent(intent);
        attempt.setEnvironment(paymentProvider.environment());
        attempt.setInternalReference(generateReference());
        attempt.setExpectedAmount(intent.getExpectedAmount());
        attempt.setCurrency(intent.getCurrency());
        attempt.setAttemptNumber(attemptNumber);
        attempt.setStatus(PaymentAttemptStatus.CREATED);
        attempt.setExpiresAt(OffsetDateTime.now().plus(paymentProperties.attemptTtl()));
        attempt = attemptRepository.saveAndFlush(attempt);
        PaystackInitializeResponse response = paymentProvider.initialize(attempt, intent.getPlan());
        if (response == null || !response.status() || response.data() == null) {
            attempt.setStatus(PaymentAttemptStatus.FAILED);
            attempt.setCompletedAt(OffsetDateTime.now());
            attempt.setFailureCode("INITIALIZE_FAILED");
            attempt.setFailureMessageSafe(PaymentFailureCategory.PROVIDER_UNAVAILABLE.name());
            notifyPayment(attempt, NotificationType.PAYMENT_FAILED, "Payment failed", "Payment initialization failed.");
            throw new IllegalStateException("Paystack initialization failed.");
        }
        attempt.setProviderReference(response.data().reference());
        attempt.setAuthorizationUrl(response.data().authorizationUrl());
        attempt.setAccessCode(response.data().accessCode());
        attempt.setInitializedAt(OffsetDateTime.now());
        attempt.setStatus(PaymentAttemptStatus.PENDING);
        intent.setStatus(PaymentIntentStatus.PAYMENT_PENDING);
        auditEventService.record(user.getId(), "USER", intent.getWorkspace(), null,
                retry ? AuditEventType.PAYMENT_RETRY_INITIALIZED : AuditEventType.PAYMENT_INITIALIZED,
                "PaymentAttempt", attempt.getId(), "{\"environment\":\"TEST\"}");
        return new PaymentAttemptInitializationResponse(intent.getId(), attempt.getId(), attempt.getAttemptNumber(),
                attempt.getAuthorizationUrl(), attempt.getInternalReference(), attempt.getStatus());
    }

    private void settleSuccessfulAttempt(PaymentAttempt attempt, UUID actorId) {
        BillingPaymentIntent intent = intentRepository.findByIdForUpdate(attempt.getPaymentIntent().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment intent not found."));
        attempt.setStatus(PaymentAttemptStatus.SUCCESS);
        attempt.setCompletedAt(OffsetDateTime.now());
        auditEventService.record(actorId, "USER", intent.getWorkspace(), null, AuditEventType.PAYMENT_ATTEMPT_SUCCEEDED,
                "PaymentAttempt", attempt.getId(), "{}");
        long existingSuccesses = attemptRepository.countByPaymentIntentIdAndStatus(intent.getId(), PaymentAttemptStatus.SUCCESS);
        if (intent.getStatus() != PaymentIntentStatus.PAID) {
            intent.setStatus(PaymentIntentStatus.PAID);
            intent.setSettledAt(OffsetDateTime.now());
            if (intent.getPurchaseType() == BillingPurchaseType.AI_CREDIT_PACK && intent.getAiCreditPurchase() != null) {
                creditPurchaseServiceProvider.getObject().fulfillPurchase(intent.getAiCreditPurchase().getId(), attempt, actorId);
            } else if (intent.getPlan() != null) {
                subscriptionChangeService.activatePlan(intent.getWorkspace().getId(), intent.getPlan().getCode(),
                        intent.getBillingInterval(), attempt.getProviderReference(), actorId, "PAYMENT_SUCCESS", SubscriptionAccessSource.PAID);
                auditEventService.record(actorId, "USER", intent.getWorkspace(), null, AuditEventType.SUBSCRIPTION_UPGRADED,
                        "WorkspaceSubscription", intent.getWorkspace().getId(), "{\"plan\":\"" + intent.getPlan().getCode() + "\"}");
                notifyPayment(attempt, NotificationType.PAYMENT_SUCCESS, "Payment succeeded", "Your workspace has been upgraded to " + intent.getPlan().getName() + ".");
            }
        } else if (existingSuccesses > 0) {
            attempt.setRequiresReview(true);
            intent.setStatus(PaymentIntentStatus.REQUIRES_REVIEW);
            auditEventService.record(actorId, "USER", intent.getWorkspace(), null, AuditEventType.PAYMENT_DUPLICATE_SUCCESS_DETECTED,
                    "PaymentAttempt", attempt.getId(), "{}");
        }
    }

    private boolean isRetryable(PaymentAttempt attempt) {
        return switch (attempt.getStatus()) {
            case FAILED, ABANDONED, CANCELLED, EXPIRED, VERIFICATION_FAILED -> true;
            default -> false;
        };
    }

    private PaymentFailureCategory safeFailure(PaymentAttempt attempt) {
        if (attempt.getFailureMessageSafe() == null) return null;
        try {
            return PaymentFailureCategory.valueOf(attempt.getFailureMessageSafe());
        } catch (IllegalArgumentException ignored) {
            return PaymentFailureCategory.UNKNOWN;
        }
    }

    private PaymentAttemptInitializationResponse existingIdempotentResponse(String scope, String key) {
        if (key == null || key.isBlank()) return null;
        return idempotencyRecordRepository.findByScopeAndIdempotencyKey(scope, normalizeIdempotencyKey(key))
                .flatMap(record -> record.getPaymentAttemptId() == null ? java.util.Optional.<PaymentAttempt>empty() : attemptRepository.findById(record.getPaymentAttemptId()))
                .map(attempt -> new PaymentAttemptInitializationResponse(attempt.getPaymentIntent().getId(), attempt.getId(), attempt.getAttemptNumber(),
                        attempt.getAuthorizationUrl(), attempt.getInternalReference(), attempt.getStatus()))
                .orElse(null);
    }

    private void saveIdempotent(String scope, String key, User user, PaymentAttemptInitializationResponse response) {
        if (key == null || key.isBlank() || response.paymentAttemptId() == null) return;
        PaymentIdempotencyRecord record = new PaymentIdempotencyRecord();
        record.setScope(scope);
        record.setIdempotencyKey(normalizeIdempotencyKey(key));
        record.setUser(user);
        record.setPaymentIntentId(response.paymentIntentId());
        record.setPaymentAttemptId(response.paymentAttemptId());
        idempotencyRecordRepository.save(record);
    }

    private String normalizeIdempotencyKey(String key) {
        String normalized = key.trim();
        if (!normalized.matches("[A-Za-z0-9._:-]{1,180}")) throw new IllegalArgumentException("Invalid Idempotency-Key.");
        return normalized;
    }

    private void notifyPayment(PaymentAttempt attempt, NotificationType type, String title, String message) {
        notificationService.create(attempt.getPaymentIntent().getInitiatedBy(), attempt.getPaymentIntent().getWorkspace(), null,
                type, title, message, null, NotificationPriority.NORMAL);
    }

    @Transactional(readOnly = true)
    public Page<PaymentTransaction> transactions(UUID workspaceId, Pageable pageable) {
        return transactionRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId, pageable);
    }

    @Transactional
    public WorkspaceSubscription subscription(UUID workspaceId) {
        return freeSubscriptionProvisioningService.ensureFreeSubscription(workspaceId);
    }

    private String generateReference() {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        return "RA-" + HexFormat.of().formatHex(bytes).toUpperCase();
    }
}
