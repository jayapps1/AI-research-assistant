package com.researchassistant.usage;

import com.researchassistant.subscription.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class QuotaService {
    private final EntitlementService entitlementService;
    private final UsageAggregationService aggregationService;

    public QuotaService(EntitlementService entitlementService, UsageAggregationService aggregationService) {
        this.entitlementService = entitlementService;
        this.aggregationService = aggregationService;
    }

    public WorkspaceUsageSummary currentSummary(UUID workspaceId) {
        WorkspaceSubscription subscription = entitlementService.effectiveSubscription(workspaceId);
        OffsetDateTime start = subscription.getCurrentPeriodStart();
        OffsetDateTime end = subscription.getCurrentPeriodEnd();
        return new WorkspaceUsageSummary(
                start,
                end,
                metric(workspaceId, PlanFeature.AI_GENERATION, UsageMetricType.AI_GENERATION_REQUEST, start, end, null),
                metric(workspaceId, PlanFeature.AI_TOKENS, UsageMetricType.AI_TOTAL_TOKEN, start, end, null),
                metric(workspaceId, PlanFeature.STORAGE, UsageMetricType.STORAGE_BYTES, start, end, null),
                metric(workspaceId, PlanFeature.PROJECT_CREATION, UsageMetricType.PROJECT_COUNT, start, end, null),
                metric(workspaceId, PlanFeature.REPORT_EXPORT, UsageMetricType.REPORT_EXPORT, start, end, null)
        );
    }

    public void requireWithinQuota(UUID workspaceId, PlanFeature feature, UsageMetricType metric, long requestedQuantity) {
        requireWithinQuota(workspaceId, feature, metric, requestedQuantity, null);
    }

    public void requireWithinQuota(UUID workspaceId, PlanFeature feature, UsageMetricType metric, long requestedQuantity, UUID projectId) {
        Entitlement entitlement = entitlementService.requireFeature(workspaceId, feature);
        if (entitlement.limitValue() == null) {
            return;
        }
        WorkspaceSubscription subscription = entitlementService.effectiveSubscription(workspaceId);
        long used = aggregationService.usage(workspaceId, metric, subscription.getCurrentPeriodStart(), subscription.getCurrentPeriodEnd(), projectId);
        long remaining = Math.max(0L, entitlement.limitValue() - used);
        if (requestedQuantity > remaining) {
            throw new QuotaExceededException(feature, entitlement.limitValue(), used, remaining, subscription.getCurrentPeriodEnd());
        }
    }

    private UsageMetric metric(UUID workspaceId, PlanFeature feature, UsageMetricType type, OffsetDateTime start, OffsetDateTime end, UUID projectId) {
        Entitlement entitlement = entitlementService.getEntitlement(workspaceId, feature);
        long used = aggregationService.usage(workspaceId, type, start, end, projectId);
        Long limit = entitlement.enabled() ? entitlement.limitValue() : 0L;
        Long remaining = limit == null ? null : Math.max(0L, limit - used);
        return new UsageMetric(used, limit, remaining, entitlement.limitUnit());
    }
}
