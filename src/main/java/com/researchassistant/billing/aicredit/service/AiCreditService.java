package com.researchassistant.billing.aicredit.service;

import com.researchassistant.audit.AuditEventService;
import com.researchassistant.audit.AuditEventType;
import com.researchassistant.billing.aicredit.dto.AiCreditBalanceResponse;
import com.researchassistant.billing.aicredit.dto.AiCreditLedgerItemResponse;
import com.researchassistant.billing.aicredit.entity.*;
import com.researchassistant.billing.aicredit.exception.AiCreditsExhaustedException;
import com.researchassistant.billing.aicredit.repository.AiCreditLedgerEntryRepository;
import com.researchassistant.billing.aicredit.repository.AiCreditPackRepository;
import com.researchassistant.billing.aicredit.repository.AiCreditWalletRepository;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.subscription.*;
import com.researchassistant.usage.UsageLedgerEntry;
import com.researchassistant.usage.UsageLedgerEntryRepository;
import com.researchassistant.usage.UsageMetricType;
import com.researchassistant.workspace.entity.Workspace;
import com.researchassistant.workspace.repository.WorkspaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Transactional
public class AiCreditService {

    private static final BigDecimal SCALE_FACTOR = new BigDecimal("10000");

    private final AiCreditWalletRepository walletRepository;
    private final AiCreditLedgerEntryRepository ledgerRepository;
    private final AiCreditPackRepository packRepository;
    private final WorkspaceRepository workspaceRepository;
    private final EntitlementService entitlementService;
    private final UsageLedgerEntryRepository usageLedgerRepository;
    private final AuditEventService auditEventService;

    public AiCreditService(
            AiCreditWalletRepository walletRepository,
            AiCreditLedgerEntryRepository ledgerRepository,
            AiCreditPackRepository packRepository,
            WorkspaceRepository workspaceRepository,
            EntitlementService entitlementService,
            UsageLedgerEntryRepository usageLedgerRepository,
            AuditEventService auditEventService
    ) {
        this.walletRepository = walletRepository;
        this.ledgerRepository = ledgerRepository;
        this.packRepository = packRepository;
        this.workspaceRepository = workspaceRepository;
        this.entitlementService = entitlementService;
        this.usageLedgerRepository = usageLedgerRepository;
        this.auditEventService = auditEventService;
    }

    @Transactional(readOnly = true)
    public AiCreditBalanceResponse getBalance(UUID workspaceId) {
        AiCreditWallet wallet = walletRepository.findByWorkspaceId(workspaceId)
                .orElseGet(() -> createDefaultWalletTransient(workspaceId));

        AiCreditBalanceResponse.IncludedAllowance included = calculateIncludedAllowance(workspaceId);

        BigDecimal promotionalRemaining = wallet.getPromotionalBalance() != null ? wallet.getPromotionalBalance() : BigDecimal.ZERO;
        BigDecimal purchasedRemaining = wallet.getPurchasedBalance() != null ? wallet.getPurchasedBalance() : BigDecimal.ZERO;

        BigDecimal totalAvailable;
        if ("UNLIMITED".equalsIgnoreCase(included.limitMode())) {
            totalAvailable = BigDecimal.valueOf(999999);
        } else {
            BigDecimal inc = included.remaining() != null ? included.remaining() : BigDecimal.ZERO;
            totalAvailable = inc.add(promotionalRemaining).add(purchasedRemaining);
        }

        return new AiCreditBalanceResponse(
                included,
                new AiCreditBalanceResponse.PromotionalBalance(promotionalRemaining),
                new AiCreditBalanceResponse.PurchasedBalance(purchasedRemaining),
                totalAvailable
        );
    }

    public AiCreditReservation reserveCredits(UUID workspaceId, BigDecimal estimatedCredits) {
        if (estimatedCredits == null || estimatedCredits.compareTo(BigDecimal.ZERO) <= 0) {
            return new AiCreditReservation(workspaceId, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        // Lock wallet for update to prevent concurrent overspending
        AiCreditWallet wallet = getOrCreateWalletForUpdate(workspaceId);
        AiCreditBalanceResponse.IncludedAllowance included = calculateIncludedAllowance(workspaceId);

        if ("DISABLED".equalsIgnoreCase(included.limitMode()) && wallet.totalAvailableBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AiCreditsExhaustedException(workspaceId, BigDecimal.ZERO, wallet.getPromotionalBalance(),
                    wallet.getPurchasedBalance(), BigDecimal.ZERO, hasActivePacks());
        }

        BigDecimal reservedFromIncluded = BigDecimal.ZERO;
        BigDecimal reservedFromWallet = BigDecimal.ZERO;

        if ("UNLIMITED".equalsIgnoreCase(included.limitMode())) {
            reservedFromIncluded = estimatedCredits;
        } else {
            BigDecimal incRem = included.remaining() != null ? included.remaining() : BigDecimal.ZERO;
            if (incRem.compareTo(estimatedCredits) >= 0) {
                reservedFromIncluded = estimatedCredits;
            } else {
                reservedFromIncluded = incRem;
                BigDecimal neededFromWallet = estimatedCredits.subtract(incRem);
                BigDecimal availableInWallet = wallet.totalAvailableBalance().subtract(wallet.getReservedBalance());

                if (availableInWallet.compareTo(neededFromWallet) < 0) {
                    BigDecimal totalAvail = incRem.add(availableInWallet.max(BigDecimal.ZERO));
                    throw new AiCreditsExhaustedException(workspaceId, incRem, wallet.getPromotionalBalance(),
                            wallet.getPurchasedBalance(), totalAvail, hasActivePacks());
                }

                wallet.setReservedBalance(wallet.getReservedBalance().add(neededFromWallet));
                walletRepository.save(wallet);
                reservedFromWallet = neededFromWallet;
            }
        }

        return new AiCreditReservation(workspaceId, estimatedCredits, reservedFromIncluded, reservedFromWallet);
    }

    public void reconcileReservation(AiCreditReservation reservation, BigDecimal actualCredits, UUID aiRequestId, User user) {
        if (reservation == null) return;
        UUID workspaceId = reservation.workspaceId();
        BigDecimal actual = actualCredits != null && actualCredits.compareTo(BigDecimal.ZERO) > 0 ? actualCredits : BigDecimal.ZERO;

        AiCreditWallet wallet = getOrCreateWalletForUpdate(workspaceId);

        // Release the entire reservation from wallet
        if (reservation.reservedFromWallet() != null && reservation.reservedFromWallet().compareTo(BigDecimal.ZERO) > 0) {
            wallet.setReservedBalance(wallet.getReservedBalance().subtract(reservation.reservedFromWallet()).max(BigDecimal.ZERO));
        }

        if (actual.compareTo(BigDecimal.ZERO) == 0) {
            walletRepository.save(wallet);
            return;
        }

        // 1. Consume from included allowance first
        BigDecimal fromIncluded = actual.min(reservation.reservedFromIncluded());
        if (fromIncluded.compareTo(BigDecimal.ZERO) > 0) {
            recordIncludedUsage(workspaceId, fromIncluded, aiRequestId);
        }

        // 2. Consume remainder from wallet: promotional first, then purchased
        BigDecimal remainingToDeduct = actual.subtract(fromIncluded);
        if (remainingToDeduct.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal fromPromo = remainingToDeduct.min(wallet.getPromotionalBalance());
            if (fromPromo.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal before = wallet.getPromotionalBalance();
                BigDecimal after = before.subtract(fromPromo);
                wallet.setPromotionalBalance(after);
                recordLedgerEntry(wallet.getWorkspace(), AiCreditBucket.PROMOTIONAL, AiCreditLedgerType.CONSUMPTION,
                        fromPromo, before, after, "AI_REQUEST", aiRequestId, aiRequestId, null, null, null,
                        "CONSUMPTION:PROMO:" + aiRequestId, user);
            }

            BigDecimal fromPurchased = remainingToDeduct.subtract(fromPromo);
            if (fromPurchased.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal before = wallet.getPurchasedBalance();
                BigDecimal after = before.subtract(fromPurchased);
                wallet.setPurchasedBalance(after);
                recordLedgerEntry(wallet.getWorkspace(), AiCreditBucket.PURCHASED, AiCreditLedgerType.CONSUMPTION,
                        fromPurchased, before, after, "AI_REQUEST", aiRequestId, aiRequestId, null, null, null,
                        "CONSUMPTION:PURCHASED:" + aiRequestId, user);
            }
        }

        walletRepository.save(wallet);
    }

    public void releaseReservation(AiCreditReservation reservation) {
        if (reservation == null || reservation.reservedFromWallet() == null || reservation.reservedFromWallet().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        AiCreditWallet wallet = getOrCreateWalletForUpdate(reservation.workspaceId());
        wallet.setReservedBalance(wallet.getReservedBalance().subtract(reservation.reservedFromWallet()).max(BigDecimal.ZERO));
        walletRepository.save(wallet);
    }

    public void adminGrant(UUID workspaceId, BigDecimal creditAmount, AiCreditBucket bucket, String reason, User adminUser) {
        if (creditAmount == null || creditAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be greater than zero.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required for administrative credit grants.");
        }

        AiCreditWallet wallet = getOrCreateWalletForUpdate(workspaceId);
        AiCreditBucket targetBucket = bucket != null ? bucket : AiCreditBucket.PURCHASED;

        if (targetBucket == AiCreditBucket.PURCHASED) {
            BigDecimal before = wallet.getPurchasedBalance();
            BigDecimal after = before.add(creditAmount);
            wallet.setPurchasedBalance(after);
            recordLedgerEntry(wallet.getWorkspace(), AiCreditBucket.PURCHASED, AiCreditLedgerType.ADMIN_GRANT,
                    creditAmount, before, after, "ADMIN_GRANT", adminUser != null ? adminUser.getId() : null,
                    null, null, null, null, "ADMIN_GRANT:" + UUID.randomUUID(), adminUser);
        } else {
            BigDecimal before = wallet.getPromotionalBalance();
            BigDecimal after = before.add(creditAmount);
            wallet.setPromotionalBalance(after);
            recordLedgerEntry(wallet.getWorkspace(), AiCreditBucket.PROMOTIONAL, AiCreditLedgerType.PROMOTIONAL_GRANT,
                    creditAmount, before, after, "PROMOTIONAL_GRANT", adminUser != null ? adminUser.getId() : null,
                    null, null, null, null, "PROMO_GRANT:" + UUID.randomUUID(), adminUser);
        }

        walletRepository.save(wallet);
        auditEventService.record(adminUser != null ? adminUser.getId() : null, "ADMIN", wallet.getWorkspace(), null,
                AuditEventType.AI_CREDIT_ADMIN_GRANTED, "AiCreditWallet", wallet.getId(),
                "{\"amount\":" + creditAmount + ",\"bucket\":\"" + targetBucket + "\",\"reason\":\"" + reason + "\"}");
    }

    @Transactional(readOnly = true)
    public Page<AiCreditLedgerItemResponse> listLedger(UUID workspaceId, Pageable pageable) {
        return ledgerRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId, pageable)
                .map(this::toLedgerResponse);
    }

    public AiCreditWallet getOrCreateWalletForUpdate(UUID workspaceId) {
        return walletRepository.findByWorkspaceIdForUpdate(workspaceId)
                .orElseGet(() -> {
                    Workspace ws = workspaceRepository.findById(workspaceId)
                            .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + workspaceId));
                    AiCreditWallet w = new AiCreditWallet();
                    w.setWorkspace(ws);
                    w.setPurchasedBalance(BigDecimal.ZERO);
                    w.setPromotionalBalance(BigDecimal.ZERO);
                    w.setReservedBalance(BigDecimal.ZERO);
                    return walletRepository.saveAndFlush(w);
                });
    }

    private AiCreditBalanceResponse.IncludedAllowance calculateIncludedAllowance(UUID workspaceId) {
        OffsetDateTime now = OffsetDateTime.now();
        WorkspaceSubscription sub = entitlementService.effectiveSubscription(workspaceId);

        OffsetDateTime start = sub.getCurrentPeriodStart();
        OffsetDateTime end = sub.getCurrentPeriodEnd();

        // For yearly subscriptions, calculate the active monthly sub-window
        if (sub.getBillingInterval() == BillingInterval.YEARLY && start != null && end != null) {
            OffsetDateTime winStart = start;
            while (winStart.plusMonths(1).isBefore(now) && winStart.plusMonths(1).isBefore(end)) {
                winStart = winStart.plusMonths(1);
            }
            start = winStart;
            end = winStart.plusMonths(1).isBefore(sub.getCurrentPeriodEnd()) ? winStart.plusMonths(1) : sub.getCurrentPeriodEnd();
        }

        Entitlement creditEnt = entitlementService.getEntitlement(workspaceId, PlanFeature.AI_GENERATION_CREDITS_MONTHLY);
        Entitlement genEnt = entitlementService.getEntitlement(workspaceId, PlanFeature.AI_GENERATION);

        if (!genEnt.enabled() && !creditEnt.enabled()) {
            return new AiCreditBalanceResponse.IncludedAllowance("DISABLED", 0L, BigDecimal.ZERO, BigDecimal.ZERO, start, end);
        }

        if (creditEnt.enabled()) {
            if (creditEnt.limitValue() == null) {
                return new AiCreditBalanceResponse.IncludedAllowance("UNLIMITED", null, BigDecimal.ZERO, null, start, end);
            }
            Long limit = creditEnt.limitValue();
            long rawUsed = usageLedgerRepository.sumQuantity(workspaceId, UsageMetricType.AI_GENERATION_CREDIT, start, end);
            BigDecimal used = BigDecimal.valueOf(rawUsed).divide(SCALE_FACTOR, 4, RoundingMode.HALF_UP);
            BigDecimal remaining = BigDecimal.valueOf(limit).subtract(used).max(BigDecimal.ZERO);
            return new AiCreditBalanceResponse.IncludedAllowance("LIMITED", limit, used, remaining, start, end);
        }

        // Fallback: If AI_GENERATION is enabled but no credits limit configured
        return new AiCreditBalanceResponse.IncludedAllowance("DISABLED", 0L, BigDecimal.ZERO, BigDecimal.ZERO, start, end);
    }

    private void recordIncludedUsage(UUID workspaceId, BigDecimal amount, UUID aiRequestId) {
        Workspace ws = workspaceRepository.findById(workspaceId).orElse(null);
        if (ws == null) return;
        long scaled = amount.multiply(SCALE_FACTOR).setScale(0, RoundingMode.HALF_UP).longValue();
        if (scaled <= 0) return;

        UsageLedgerEntry entry = new UsageLedgerEntry();
        entry.setWorkspace(ws);
        entry.setMetric(UsageMetricType.AI_GENERATION_CREDIT);
        entry.setQuantity(scaled);
        entry.setSourceType("AI_REQUEST");
        entry.setSourceId(aiRequestId);
        entry.setIdempotencyKey("INCLUDED_CREDIT:" + aiRequestId);
        entry.setOccurredAt(OffsetDateTime.now());
        usageLedgerRepository.save(entry);
    }

    public void recordLedgerEntry(
            Workspace workspace,
            AiCreditBucket bucket,
            AiCreditLedgerType type,
            BigDecimal creditAmount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String sourceType,
            UUID sourceId,
            UUID aiRequestId,
            UUID paymentIntentId,
            UUID paymentAttemptId,
            UUID purchaseId,
            String idempotencyKey,
            User user
    ) {
        AiCreditLedgerEntry entry = new AiCreditLedgerEntry();
        entry.setWorkspace(workspace);
        entry.setBucket(bucket);
        entry.setType(type);
        entry.setCreditAmount(creditAmount);
        entry.setBalanceBefore(balanceBefore);
        entry.setBalanceAfter(balanceAfter);
        entry.setSourceType(sourceType);
        entry.setSourceId(sourceId);
        entry.setAiRequestId(aiRequestId);
        entry.setPaymentIntentId(paymentIntentId);
        entry.setPaymentAttemptId(paymentAttemptId);
        entry.setPurchaseId(purchaseId);
        entry.setIdempotencyKey(idempotencyKey);
        entry.setCreatedBy(user);
        ledgerRepository.save(entry);
    }

    private boolean hasActivePacks() {
        return !packRepository.findAllByActiveTrueOrderByDisplayOrderAscCreatedAtAsc().isEmpty();
    }

    private AiCreditWallet createDefaultWalletTransient(UUID workspaceId) {
        AiCreditWallet w = new AiCreditWallet();
        w.setPurchasedBalance(BigDecimal.ZERO);
        w.setPromotionalBalance(BigDecimal.ZERO);
        w.setReservedBalance(BigDecimal.ZERO);
        return w;
    }

    private AiCreditLedgerItemResponse toLedgerResponse(AiCreditLedgerEntry e) {
        return new AiCreditLedgerItemResponse(
                e.getId(),
                e.getBucket().name(),
                e.getType().name(),
                e.getCreditAmount(),
                e.getBalanceBefore(),
                e.getBalanceAfter(),
                e.getSourceType(),
                e.getSourceId(),
                e.getAiRequestId(),
                e.getPaymentIntentId(),
                e.getPaymentAttemptId(),
                e.getPurchaseId(),
                e.getCreatedBy() != null ? (e.getCreatedBy().getFirstName() + " " + e.getCreatedBy().getLastName()).trim() : null,
                e.getCreatedAt()
        );
    }
}
