package com.researchassistant.subscription.dto;

import com.researchassistant.subscription.LimitUnit;
import com.researchassistant.subscription.PlanFeature;

public record EntitlementResponse(
        PlanFeature feature,
        boolean enabled,
        Long limit,
        LimitUnit unit
) {
}
