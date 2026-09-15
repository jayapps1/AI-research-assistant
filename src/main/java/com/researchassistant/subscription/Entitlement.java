package com.researchassistant.subscription;

public record Entitlement(
        PlanFeature feature,
        boolean enabled,
        Long limitValue,
        LimitUnit limitUnit
) {
}
