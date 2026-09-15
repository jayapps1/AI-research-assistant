package com.researchassistant.subscription;

import com.researchassistant.audit.AuditEventService;
import com.researchassistant.audit.AuditEventType;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.workspace.entity.Workspace;
import com.researchassistant.workspace.repository.WorkspaceRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Transactional
public class SubscriptionChangeService {
    private final WorkspaceRepository workspaceRepository;
    private final SubscriptionPlanRepository planRepository;
    private final WorkspaceSubscriptionRepository subscriptionRepository;
    private final WorkspaceSubscriptionHistoryRepository historyRepository;
    private final AuditEventService auditEventService;

    public SubscriptionChangeService(WorkspaceRepository workspaceRepository,
                                     SubscriptionPlanRepository planRepository,
                                     WorkspaceSubscriptionRepository subscriptionRepository,
                                     WorkspaceSubscriptionHistoryRepository historyRepository,
                                     AuditEventService auditEventService) {
        this.workspaceRepository = workspaceRepository;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.historyRepository = historyRepository;
        this.auditEventService = auditEventService;
    }

    @CacheEvict(cacheNames = "subscription:workspace-entitlements", allEntries = true)
    public WorkspaceSubscription activatePlan(UUID workspaceId, String planCode, BillingInterval interval,
                                              String externalReference, UUID actorId, String reason) {
        return activatePlan(workspaceId, planCode, interval, externalReference, actorId, reason,
                externalReference == null ? SubscriptionAccessSource.FREE_DEFAULT : SubscriptionAccessSource.PAID);
    }

    @CacheEvict(cacheNames = "subscription:workspace-entitlements", allEntries = true)
    public WorkspaceSubscription activatePlan(UUID workspaceId, String planCode, BillingInterval interval,
                                              String externalReference, UUID actorId, String reason,
                                              SubscriptionAccessSource accessSource) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found."));
        SubscriptionPlan plan = planRepository.findByCodeIgnoreCase(planCode)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found."));
        OffsetDateTime now = OffsetDateTime.now();
        subscriptionRepository.findCurrentEffective(workspaceId, now)
                .ifPresent(current -> {
                    current.setStatus(WorkspaceSubscriptionStatus.CANCELLED);
                    current.setCancelledAt(now);
                    current.setCancelAt(now);
                });
        WorkspaceSubscription subscription = new WorkspaceSubscription();
        subscription.setWorkspace(workspace);
        subscription.setPlan(plan);
        subscription.setStatus(WorkspaceSubscriptionStatus.ACTIVE);
        subscription.setBillingInterval(interval);
        subscription.setStartsAt(now);
        subscription.setCurrentPeriodStart(now);
        subscription.setCurrentPeriodEnd(periodEnd(now, interval));
        subscription.setAutoRenew(interval != BillingInterval.NONE);
        subscription.setExternalSubscriptionReference(externalReference);
        subscription.setAccessSource(accessSource == null ? SubscriptionAccessSource.FREE_DEFAULT : accessSource);
        WorkspaceSubscription saved = subscriptionRepository.save(subscription);
        history(saved, plan, now, reason == null ? "PLAN_ACTIVATED" : reason);
        auditEventService.record(actorId, "USER", workspace, null, AuditEventType.SUBSCRIPTION_CHANGED,
                "WorkspaceSubscription", saved.getId(), "{\"plan\":\"" + plan.getCode() + "\"}");
        return saved;
    }

    @CacheEvict(cacheNames = "subscription:workspace-entitlements", allEntries = true)
    public WorkspaceSubscription cancel(UUID workspaceId, UUID actorId) {
        WorkspaceSubscription subscription = subscriptionRepository
                .findCurrentEffective(workspaceId, OffsetDateTime.now())
                .orElseThrow(() -> new ResourceNotFoundException("Active subscription not found."));
        subscription.setAutoRenew(false);
        subscription.setCancelAt(subscription.getCurrentPeriodEnd());
        subscription.setCancelledAt(OffsetDateTime.now());
        auditEventService.record(actorId, "USER", subscription.getWorkspace(), null, AuditEventType.SUBSCRIPTION_CANCELLED,
                "WorkspaceSubscription", subscription.getId(), "{}");
        return subscription;
    }

    private void history(WorkspaceSubscription subscription, SubscriptionPlan plan, OffsetDateTime effectiveFrom, String reason) {
        WorkspaceSubscriptionHistory history = new WorkspaceSubscriptionHistory();
        history.setWorkspace(subscription.getWorkspace());
        history.setSubscription(subscription);
        history.setPlan(plan);
        history.setEffectiveFrom(effectiveFrom);
        history.setEffectiveTo(subscription.getCurrentPeriodEnd());
        history.setReason(reason);
        historyRepository.save(history);
    }

    private OffsetDateTime periodEnd(OffsetDateTime start, BillingInterval interval) {
        return switch (interval) {
            case YEARLY -> start.plusYears(1);
            case MONTHLY, NONE -> start.plusMonths(1);
        };
    }
}
