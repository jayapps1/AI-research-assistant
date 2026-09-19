package com.researchassistant.subscription;

import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.workspace.entity.Workspace;
import com.researchassistant.workspace.repository.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Ensures that every workspace in the system has effective subscription access.
 * If a workspace lacks an active paid or complimentary subscription, it is idempotently
 * provisioned with the standard FREE plan with FREE_DEFAULT access source.
 */
@Service
public class FreeSubscriptionProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(FreeSubscriptionProvisioningService.class);
    public static final String FREE_PLAN_CODE = "FREE";

    private final WorkspaceSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final WorkspaceRepository workspaceRepository;

    public FreeSubscriptionProvisioningService(
            WorkspaceSubscriptionRepository subscriptionRepository,
            SubscriptionPlanRepository planRepository,
            WorkspaceRepository workspaceRepository
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.workspaceRepository = workspaceRepository;
    }

    /**
     * Idempotently ensures the workspace has an active subscription.
     * If an active subscription already exists (PAID, COMPLIMENTARY, or FREE), it is preserved.
     * If no active subscription exists, a real FREE subscription row is created in PostgreSQL.
     *
     * @param workspaceId the target workspace ID
     * @return the active WorkspaceSubscription
     */
    @Transactional
    @CacheEvict(cacheNames = "subscription:workspace-entitlements", allEntries = true)
    public WorkspaceSubscription ensureFreeSubscription(UUID workspaceId) {
        OffsetDateTime now = OffsetDateTime.now();
        Optional<WorkspaceSubscription> activeOpt = subscriptionRepository.findActiveSubscription(workspaceId);

        if (activeOpt.isPresent()) {
            WorkspaceSubscription existing = activeOpt.get();
            // If existing is FREE and period expired, roll it forward to current month so quotas reset smoothly
            if (FREE_PLAN_CODE.equalsIgnoreCase(existing.getPlan().getCode()) ||
                    existing.getAccessSource() == SubscriptionAccessSource.FREE_DEFAULT) {
                if (existing.getCurrentPeriodEnd() != null && existing.getCurrentPeriodEnd().isBefore(now)) {
                    OffsetDateTime periodStart = now.withDayOfMonth(1).toLocalDate().atStartOfDay().atOffset(now.getOffset());
                    existing.setCurrentPeriodStart(periodStart);
                    existing.setCurrentPeriodEnd(periodStart.plusMonths(1));
                    return subscriptionRepository.save(existing);
                }
                return existing;
            }

            // If existing is a PAID subscription whose period has ended, mark as EXPIRED and fall back to FREE
            if (existing.getCurrentPeriodEnd() != null && !existing.getCurrentPeriodEnd().isAfter(now)) {
                existing.setStatus(WorkspaceSubscriptionStatus.EXPIRED);
                subscriptionRepository.saveAndFlush(existing);
                log.info("Subscription {} for workspace {} has reached period end and is marked EXPIRED. Falling back to FREE.", existing.getId(), workspaceId);
            } else {
                return existing;
            }
        }

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + workspaceId));

        SubscriptionPlan freePlan = planRepository.findByCodeIgnoreCase(FREE_PLAN_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("FREE subscription plan is not configured in database."));

        WorkspaceSubscription subscription = new WorkspaceSubscription();
        subscription.setWorkspace(workspace);
        subscription.setPlan(freePlan);
        subscription.setStatus(WorkspaceSubscriptionStatus.ACTIVE);
        subscription.setBillingInterval(BillingInterval.NONE);
        subscription.setStartsAt(now);
        OffsetDateTime periodStart = now.withDayOfMonth(1).toLocalDate().atStartOfDay().atOffset(now.getOffset());
        subscription.setCurrentPeriodStart(periodStart);
        subscription.setCurrentPeriodEnd(periodStart.plusMonths(1));
        subscription.setAutoRenew(false);
        subscription.setAccessSource(SubscriptionAccessSource.FREE_DEFAULT);

        WorkspaceSubscription saved = subscriptionRepository.save(subscription);
        log.info("Provisioned default FREE subscription {} for workspace {}", saved.getId(), workspaceId);
        return saved;
    }

    /**
     * Backfills all active workspaces that do not currently possess an active subscription.
     * Invoked safely at application startup.
     */
    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void backfillAllWorkspacesWithoutSubscription() {
        try {
            List<Workspace> unprovisioned = workspaceRepository.findWorkspacesWithoutActiveSubscription();
            if (unprovisioned.isEmpty()) {
                log.debug("All active workspaces already possess an active subscription.");
                return;
            }

            log.info("Found {} active workspace(s) without an active subscription. Backfilling FREE plan...", unprovisioned.size());
            int backfilled = 0;
            for (Workspace ws : unprovisioned) {
                try {
                    ensureFreeSubscription(ws.getId());
                    backfilled++;
                } catch (Exception ex) {
                    log.error("Failed to backfill FREE subscription for workspace {}: {}", ws.getId(), ex.getMessage());
                }
            }
            log.info("Successfully reconciled FREE subscriptions for {} workspace(s).", backfilled);
        } catch (Exception ex) {
            log.warn("Workspace subscription reconciliation skipped during startup: {}", ex.getMessage());
        }
    }
}
