package com.researchassistant.ai.usage.dto;

import java.util.UUID;

public record AiUsageSummaryResponse(
        UUID userId,
        UUID workspaceId,
        UUID projectId,
        long requestsToday,
        long requestsThisMonth,
        long tokensThisMonth,
        long failedRequestsThisMonth
) {
}
