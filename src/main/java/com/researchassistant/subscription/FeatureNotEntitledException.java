package com.researchassistant.subscription;

public class FeatureNotEntitledException extends RuntimeException {
    private final PlanFeature feature;

    public FeatureNotEntitledException(PlanFeature feature) {
        super("Feature is not enabled for the current subscription.");
        this.feature = feature;
    }

    public PlanFeature feature() {
        return feature;
    }
}
