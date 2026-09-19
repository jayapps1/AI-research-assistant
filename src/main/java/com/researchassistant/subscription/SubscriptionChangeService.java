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
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SubscriptionChangeService {
    private final WorkspaceRepository workspaceRepository;
    private final SubscriptionPlanRepository planRepository;
    private final WorkspaceSubscriptionRepository subscriptionRepository;
    private final WorkspaceSubscriptionHistoryRepository historyRepository;
    private final AuditEventService auditEventService;
    private final SubscriptionPeriodService subscriptionPeriodService;

    public SubscriptionChangeService(WorkspaceRepository workspaceRepository,
                                     SubscriptionPlanRepository planRepository,
                                     WorkspaceSubscriptionRepository subscriptionRepository,
                                     WorkspaceSubscriptionHistoryRepository historyRepository,
                                     AuditEventService auditEventService,
                                     SubscriptionPeriodService subscriptionPeriodService) {
        this.workspaceRepository = workspaceRepository;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.historyRepository = historyRepository;
        this.auditEventService = auditEventService;
        this.subscriptionPeriodService = subscriptionPeriodService;
    }

    @CacheEvict(cacheNames = {"subscription:workspace-entitlements", "public:pricing"}, allEntries = true)
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
        List<WorkspaceSubscription> existingActive = subscriptionRepository.findActiveSubscriptions(workspaceId);

        WorkspaceSubscription samePlanActive = null;
        for (WorkspaceSubscription current : existingActive) {
            // Check if this is an early renewal of the exact same plan and interval
            if (current.getPlan().getCode().equalsIgnoreCase(plan.getCode())
                    && current.getBillingInterval() == interval
                    && current.getCurrentPeriodEnd() != null
                    && current.getCurrentPeriodEnd().isAfter(now)) {
                samePlanActive = current;
                break;
            }
        }

        if (samePlanActive != null) {
            // Early renewal of the same plan: extend currentPeriodEnd from existing period end
            OffsetDateTime oldEnd = samePlanActive.getCurrentPeriodEnd();
            SubscriptionPeriodService.RenewalPeriod renewal = subscriptionPeriodService.calculateRenewalPeriod(oldEnd, now, interval);
            samePlanActive.setCurrentPeriodEnd(renewal.periodEnd());
            if (externalReference != null) {
                samePlanActive.setExternalSubscriptionReference(externalReference);
            }
            if (accessSource != null) {
                samePlanActive.setAccessSource(accessSource);
            }
            samePlanActive.setAutoRenew(interval != BillingInterval.NONE);
            WorkspaceSubscription saved = subscriptionRepository.saveAndFlush(samePlanActive);
            history(saved, plan, oldEnd, reason == null ? "PLAN_RENEWED" : reason);
            auditEventService.record(actorId, "USER", workspace, null, AuditEventType.SUBSCRIPTION_CHANGED,
                    "WorkspaceSubscription", saved.getId(), "{\"plan\":\"" + plan.getCode() + "\",\"action\":\"RENEWED\",\"newPeriodEnd\":\"" + renewal.periodEnd() + "\"}");
            return saved;
        }

        // Otherwise (plan change, upgrade, or expired renewal): cancel previous subscriptions
        for (WorkspaceSubscription current : existingActive) {
            current.setStatus(WorkspaceSubscriptionStatus.CANCELLED);
            current.setCancelledAt(now);
            current.setCancelAt(now);
            subscriptionRepository.saveAndFlush(current);
        }

        SubscriptionPeriodService.RenewalPeriod period = subscriptionPeriodService.calculateRenewalPeriod(null, now, interval);
        WorkspaceSubscription subscription = new WorkspaceSubscription();
        subscription.setWorkspace(workspace);
        subscription.setPlan(plan);
        subscription.setStatus(WorkspaceSubscriptionStatus.ACTIVE);
        subscription.setBillingInterval(interval);
        subscription.setStartsAt(now);
        subscription.setCurrentPeriodStart(period.periodStart());
        subscription.setCurrentPeriodEnd(period.periodEnd());
        subscription.setAutoRenew(interval != BillingInterval.NONE);
        subscription.setExternalSubscriptionReference(externalReference);
        subscription.setAccessSource(accessSource == null ? SubscriptionAccessSource.FREE_DEFAULT : accessSource);
        WorkspaceSubscription saved = subscriptionRepository.saveAndFlush(subscription);
        history(saved, plan, now, reason == null ? "PLAN_ACTIVATED" : reason);
        auditEventService.record(actorId, "USER", workspace, null, AuditEventType.SUBSCRIPTION_CHANGED,
                "WorkspaceSubscription", saved.getId(), "{\"plan\":\"" + plan.getCode() + "\"}");
        return saved;
    }

    @CacheEvict(cacheNames = {"subscription:workspace-entitlements", "public:pricing"}, allEntries = true)
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
}
