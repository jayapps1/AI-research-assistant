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
    private final FreeSubscriptionProvisioningService freeSubscriptionProvisioningService;

    public EntitlementService(
            WorkspaceSubscriptionRepository subscriptionRepository,
            SubscriptionPlanRepository planRepository,
            PlanEntitlementRepository entitlementRepository,
            WorkspaceRepository workspaceRepository,
            ComplimentaryAccessGrantRepository complimentaryAccessGrantRepository,
            EntitlementOverrideRepository entitlementOverrideRepository,
            FreeSubscriptionProvisioningService freeSubscriptionProvisioningService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.entitlementRepository = entitlementRepository;
        this.workspaceRepository = workspaceRepository;
        this.complimentaryAccessGrantRepository = complimentaryAccessGrantRepository;
        this.entitlementOverrideRepository = entitlementOverrideRepository;
        this.freeSubscriptionProvisioningService = freeSubscriptionProvisioningService;
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
                .orElseGet(() -> freeSubscriptionProvisioningService.ensureFreeSubscription(workspaceId));
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
        return freeSubscriptionProvisioningService.ensureFreeSubscription(workspaceId);
    }
}
