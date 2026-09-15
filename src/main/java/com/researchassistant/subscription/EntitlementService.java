package com.researchassistant.subscription;

import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.workspace.entity.Workspace;
import com.researchassistant.workspace.repository.WorkspaceRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EntitlementService {
    private static final String FREE_PLAN_CODE = "FREE";

    private final WorkspaceSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final PlanEntitlementRepository entitlementRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ComplimentaryAccessGrantRepository complimentaryAccessGrantRepository;
    private final EntitlementOverrideRepository entitlementOverrideRepository;

    public EntitlementService(
            WorkspaceSubscriptionRepository subscriptionRepository,
            SubscriptionPlanRepository planRepository,
            PlanEntitlementRepository entitlementRepository,
            WorkspaceRepository workspaceRepository,
            ComplimentaryAccessGrantRepository complimentaryAccessGrantRepository,
            EntitlementOverrideRepository entitlementOverrideRepository
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.entitlementRepository = entitlementRepository;
        this.workspaceRepository = workspaceRepository;
        this.complimentaryAccessGrantRepository = complimentaryAccessGrantRepository;
        this.entitlementOverrideRepository = entitlementOverrideRepository;
    }

    public boolean isFeatureEnabled(UUID workspaceId, PlanFeature feature) {
        return getEntitlement(workspaceId, feature).enabled();
    }

    public Long getLimit(UUID workspaceId, PlanFeature feature) {
        Entitlement entitlement = getEntitlement(workspaceId, feature);
        if (!entitlement.enabled()) {
            return 0L;
        }
        return entitlement.limitValue();
    }

    public Entitlement requireFeature(UUID workspaceId, PlanFeature feature) {
        Entitlement entitlement = getEntitlement(workspaceId, feature);
        if (!entitlement.enabled()) {
            throw new FeatureNotEntitledException(feature);
        }
        return entitlement;
    }

    @Cacheable(cacheNames = "subscription:workspace-entitlements", key = "#workspaceId + ':' + #feature")
    public Entitlement getEntitlement(UUID workspaceId, PlanFeature feature) {
        OffsetDateTime now = OffsetDateTime.now();
        var override = entitlementOverrideRepository.findActive(workspaceId, feature, now);
        if (override.isPresent()) {
            EntitlementOverride row = override.get();
            return switch (row.getLimitMode()) {
                case DISABLED -> new Entitlement(feature, false, 0L, row.getLimitUnit());
                case UNLIMITED -> new Entitlement(feature, true, null, row.getLimitUnit());
                case LIMITED -> new Entitlement(feature, true, row.getLimitValue(), row.getLimitUnit());
            };
        }
        SubscriptionPlan plan = effectivePlan(workspaceId);
        return entitlementRepository.findByPlanIdAndFeature(plan.getId(), feature)
                .map(row -> new Entitlement(row.getFeature(), row.isEnabled(), row.getLimitValue(), row.getLimitUnit()))
                .orElse(new Entitlement(feature, false, 0L, LimitUnit.NONE));
    }

    @Cacheable(cacheNames = "subscription:workspace-entitlements", key = "#workspaceId + ':all'")
    public List<Entitlement> listEntitlements(UUID workspaceId) {
        SubscriptionPlan plan = effectivePlan(workspaceId);
        return entitlementRepository.findAllByPlanId(plan.getId())
                .stream()
                .map(row -> new Entitlement(row.getFeature(), row.isEnabled(), row.getLimitValue(), row.getLimitUnit()))
                .toList();
    }

    public WorkspaceSubscription effectiveSubscription(UUID workspaceId) {
        return subscriptionRepository.findCurrentEffective(workspaceId, OffsetDateTime.now())
                .orElseGet(() -> createFreeSubscription(workspaceId));
    }

    public SubscriptionPlan effectivePlan(UUID workspaceId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<ComplimentaryAccessGrant> grants = complimentaryAccessGrantRepository.findActiveWorkspacePlanGrants(workspaceId, now);
        if (!grants.isEmpty()) {
            return grants.getFirst().getPlan();
        }
        return effectiveSubscription(workspaceId).getPlan();
    }

    @Transactional
    public WorkspaceSubscription ensureFreeSubscription(UUID workspaceId) {
        return subscriptionRepository.findCurrentEffective(workspaceId, OffsetDateTime.now())
                .orElseGet(() -> createFreeSubscription(workspaceId));
    }

    @Transactional
    @CacheEvict(cacheNames = "subscription:workspace-entitlements", allEntries = true)
    protected WorkspaceSubscription createFreeSubscription(UUID workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found."));
        SubscriptionPlan freePlan = planRepository.findByCodeIgnoreCase(FREE_PLAN_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("FREE subscription plan is not configured."));
        OffsetDateTime now = OffsetDateTime.now();
        WorkspaceSubscription subscription = new WorkspaceSubscription();
        subscription.setWorkspace(workspace);
        subscription.setPlan(freePlan);
        subscription.setStatus(WorkspaceSubscriptionStatus.ACTIVE);
        subscription.setBillingInterval(BillingInterval.NONE);
        subscription.setStartsAt(now);
        subscription.setCurrentPeriodStart(now.withDayOfMonth(1).toLocalDate().atStartOfDay().atOffset(now.getOffset()));
        subscription.setCurrentPeriodEnd(subscription.getCurrentPeriodStart().plusMonths(1));
        subscription.setAutoRenew(false);
        subscription.setAccessSource(SubscriptionAccessSource.FREE_DEFAULT);
        return subscriptionRepository.save(subscription);
    }
}
