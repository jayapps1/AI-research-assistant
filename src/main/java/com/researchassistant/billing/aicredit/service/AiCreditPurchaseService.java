package com.researchassistant.billing.aicredit.service;

import com.researchassistant.audit.AuditEventService;
import com.researchassistant.audit.AuditEventType;
import com.researchassistant.billing.*;
import com.researchassistant.billing.aicredit.dto.BuyAiCreditPackRequest;
import com.researchassistant.billing.aicredit.entity.*;
import com.researchassistant.billing.aicredit.repository.AiCreditPackRepository;
import com.researchassistant.billing.aicredit.repository.AiCreditPurchaseRepository;
import com.researchassistant.billing.aicredit.repository.AiCreditWalletRepository;
import com.researchassistant.billing.dto.BillingDtos.PaymentAttemptInitializationResponse;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.notification.NotificationPriority;
import com.researchassistant.notification.NotificationService;
import com.researchassistant.notification.NotificationType;
import com.researchassistant.workspace.entity.Workspace;
import com.researchassistant.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Transactional
public class AiCreditPurchaseService {

    private final AiCreditPackRepository packRepository;
    private final AiCreditPurchaseRepository purchaseRepository;
    private final AiCreditWalletRepository walletRepository;
    private final AiCreditService creditService;
    private final WorkspaceRepository workspaceRepository;
    private final BillingPaymentIntentRepository intentRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final PaymentProvider paymentProvider;
    private final PaymentProperties paymentProperties;
    private final NotificationService notificationService;
    private final AuditEventService auditEventService;
    private final SecureRandom random = new SecureRandom();

    public AiCreditPurchaseService(
            AiCreditPackRepository packRepository,
            AiCreditPurchaseRepository purchaseRepository,
            AiCreditWalletRepository walletRepository,
            AiCreditService creditService,
            WorkspaceRepository workspaceRepository,
            BillingPaymentIntentRepository intentRepository,
            PaymentAttemptRepository attemptRepository,
            PaymentProvider paymentProvider,
            PaymentProperties paymentProperties,
            NotificationService notificationService,
            AuditEventService auditEventService
    ) {
        this.packRepository = packRepository;
        this.purchaseRepository = purchaseRepository;
        this.walletRepository = walletRepository;
        this.creditService = creditService;
        this.workspaceRepository = workspaceRepository;
        this.intentRepository = intentRepository;
        this.attemptRepository = attemptRepository;
        this.paymentProvider = paymentProvider;
        this.paymentProperties = paymentProperties;
        this.notificationService = notificationService;
        this.auditEventService = auditEventService;
    }

    public PaymentAttemptInitializationResponse initializePurchase(UUID workspaceId, User user, BuyAiCreditPackRequest request) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + workspaceId));

        AiCreditPack pack = packRepository.findByCodeIgnoreCase(request.creditPackCode())
                .orElseThrow(() -> new ResourceNotFoundException("AI credit pack not found: " + request.creditPackCode()));

        if (!pack.isActive()) {
            throw new IllegalArgumentException("AI credit pack '" + pack.getCode() + "' is currently inactive.");
        }

        AiCreditPurchase purchase = new AiCreditPurchase();
        purchase.setWorkspace(workspace);
        purchase.setCreditPack(pack);
        purchase.setPackCode(pack.getCode());
        purchase.setPackName(pack.getName());
        purchase.setCreditsPurchased(pack.getCreditAmount());
        purchase.setPriceAmount(pack.getPriceAmount());
        purchase.setCurrency(pack.getCurrency());
        purchase.setStatus(AiCreditPurchaseStatus.CREATED);
        purchase = purchaseRepository.saveAndFlush(purchase);

        BillingPaymentIntent intent = new BillingPaymentIntent();
        intent.setWorkspace(workspace);
        intent.setInitiatedBy(user);
        intent.setPurchaseType(BillingPurchaseType.AI_CREDIT_PACK);
        intent.setAiCreditPurchase(purchase);
        intent.setPlan(null);
        intent.setBillingInterval(null);
        intent.setExpectedAmount(pack.getPriceAmount());
        intent.setBaseAmount(pack.getPriceAmount());
        intent.setProcessingFeeAmount(BigDecimal.ZERO);
        intent.setAiGenerationFeeAmount(BigDecimal.ZERO);
        intent.setTotalAmount(pack.getPriceAmount());
        intent.setCurrency(pack.getCurrency());
        intent.setStatus(PaymentIntentStatus.OPEN);
        intent.setExpiresAt(OffsetDateTime.now().plus(paymentProperties.intentTtl()));
        intent = intentRepository.saveAndFlush(intent);

        purchase.setBillingPaymentIntent(intent);
        purchaseRepository.save(purchase);

        auditEventService.record(user.getId(), "USER", workspace, null, AuditEventType.AI_CREDIT_PURCHASE_CREATED,
                "AiCreditPurchase", purchase.getId(),
                "{\"pack\":\"" + pack.getCode() + "\",\"credits\":" + pack.getCreditAmount() + ",\"price\":" + pack.getPriceAmount() + "}");

        return initializeAttempt(intent, user, pack);
    }

    public void fulfillPurchase(UUID purchaseId, PaymentAttempt attempt, UUID actorId) {
        AiCreditPurchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new ResourceNotFoundException("AI credit purchase not found: " + purchaseId));

        // EXACTLY-ONCE GUARANTEE: Webhook replay or duplicate callback must not grant credits twice
        if (purchase.getStatus() == AiCreditPurchaseStatus.CREDITED) {
            return;
        }

        purchase.setStatus(AiCreditPurchaseStatus.CREDITED);
        purchase.setCreditedAt(OffsetDateTime.now());
        purchaseRepository.save(purchase);

        AiCreditWallet wallet = creditService.getOrCreateWalletForUpdate(purchase.getWorkspace().getId());
        BigDecimal before = wallet.getPurchasedBalance() != null ? wallet.getPurchasedBalance() : BigDecimal.ZERO;
        BigDecimal after = before.add(purchase.getCreditsPurchased());
        wallet.setPurchasedBalance(after);
        walletRepository.save(wallet);

        creditService.recordLedgerEntry(
                purchase.getWorkspace(),
                AiCreditBucket.PURCHASED,
                AiCreditLedgerType.PURCHASE,
                purchase.getCreditsPurchased(),
                before,
                after,
                "AI_CREDIT_PURCHASE",
                purchase.getId(),
                null,
                attempt.getPaymentIntent().getId(),
                attempt.getId(),
                purchase.getId(),
                "PURCHASE:" + purchase.getId(),
                attempt.getPaymentIntent().getInitiatedBy()
        );

        auditEventService.record(actorId, "USER", purchase.getWorkspace(), null, AuditEventType.AI_CREDIT_PURCHASE_CREDITED,
                "AiCreditPurchase", purchase.getId(),
                "{\"pack\":\"" + purchase.getPackCode() + "\",\"credits\":" + purchase.getCreditsPurchased() + "}");

        notificationService.create(
                attempt.getPaymentIntent().getInitiatedBy(),
                purchase.getWorkspace(),
                null,
                NotificationType.PAYMENT_SUCCESS,
                "AI Credits Added",
                purchase.getCreditsPurchased().stripTrailingZeros().toPlainString() + " AI credits have been added to your workspace balance.",
                null,
                NotificationPriority.NORMAL
        );
    }

    private PaymentAttemptInitializationResponse initializeAttempt(BillingPaymentIntent intent, User user, AiCreditPack pack) {
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

        PaystackInitializeResponse response = paymentProvider.initialize(attempt, null);
        if (response == null || !response.status() || response.data() == null) {
            attempt.setStatus(PaymentAttemptStatus.FAILED);
            attempt.setCompletedAt(OffsetDateTime.now());
            attempt.setFailureCode("INITIALIZE_FAILED");
            attempt.setFailureMessageSafe(PaymentFailureCategory.PROVIDER_UNAVAILABLE.name());
            throw new IllegalStateException("Paystack initialization failed.");
        }

        attempt.setProviderReference(response.data().reference());
        attempt.setAuthorizationUrl(response.data().authorizationUrl());
        attempt.setAccessCode(response.data().accessCode());
        attempt.setInitializedAt(OffsetDateTime.now());
        attempt.setStatus(PaymentAttemptStatus.PENDING);
        intent.setStatus(PaymentIntentStatus.PAYMENT_PENDING);

        auditEventService.record(user.getId(), "USER", intent.getWorkspace(), null, AuditEventType.PAYMENT_INITIALIZED,
                "PaymentAttempt", attempt.getId(), "{\"environment\":\"TEST\",\"type\":\"AI_CREDIT_PACK\"}");

        return new PaymentAttemptInitializationResponse(
                intent.getId(),
                attempt.getId(),
                attempt.getAttemptNumber(),
                attempt.getAuthorizationUrl(),
                attempt.getInternalReference(),
                attempt.getStatus()
        );
    }

    private String generateReference() {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        return "RA-AIC-" + HexFormat.of().formatHex(bytes).toUpperCase();
    }
}
