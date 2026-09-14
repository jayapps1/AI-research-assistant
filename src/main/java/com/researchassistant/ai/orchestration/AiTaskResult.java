package com.researchassistant.ai.orchestration;

import com.researchassistant.ai.provider.AiProviderType;
import com.researchassistant.ai.usage.AiRequestStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AiTaskResult<T>(
        UUID requestId,
        AiTaskType taskType,
        AiProviderType provider,
        String model,
        AiRequestStatus status,
        T result,
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens,
        Long latencyMs,
        String failureCategory,
        String failureCode,
        String providerRequestId,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        List<String> warnings
) {
}
