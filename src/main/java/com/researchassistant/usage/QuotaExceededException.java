package com.researchassistant.usage;

import com.researchassistant.subscription.PlanFeature;

import java.time.OffsetDateTime;

public class QuotaExceededException extends RuntimeException {
    private final PlanFeature feature;
    private final long limit;
    private final long used;
    private final long remaining;
    private final OffsetDateTime resetAt;

    public QuotaExceededException(PlanFeature feature, long limit, long used, long remaining, OffsetDateTime resetAt) {
        super("QUOTA_EXCEEDED");
        this.feature = feature;
        this.limit = limit;
        this.used = used;
        this.remaining = remaining;
        this.resetAt = resetAt;
    }

    public PlanFeature feature() { return feature; }
    public long limit() { return limit; }
    public long used() { return used; }
    public long remaining() { return remaining; }
    public OffsetDateTime resetAt() { return resetAt; }
}
