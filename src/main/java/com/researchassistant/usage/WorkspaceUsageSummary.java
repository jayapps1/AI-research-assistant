package com.researchassistant.usage;

import java.time.OffsetDateTime;

public record WorkspaceUsageSummary(
        OffsetDateTime periodStart,
        OffsetDateTime periodEnd,
        UsageMetric aiRequests,
        UsageMetric aiTokens,
        UsageMetric storage,
        UsageMetric projects,
        UsageMetric exports
) {
}
