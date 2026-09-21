package com.researchassistant.ai.orchestration;

import com.researchassistant.ai.provider.AiProviderType;
import com.researchassistant.ai.usage.AiRequestStatus;

import java.time.OffsetDateTime;
import java.util.Collections;
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
        List<String> warnings,
        Integer cachedInputTokens,
        Throwable cause
) {
    public AiTaskResult(
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
            List<String> warnings,
            Integer cachedInputTokens
    ) {
        this(requestId, taskType, provider, model, status, result, inputTokens, outputTokens, totalTokens,
                latencyMs, failureCategory, failureCode, providerRequestId, startedAt, completedAt,
                warnings != null ? warnings : Collections.emptyList(), cachedInputTokens, null);
    }

    public AiTaskResult(
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
        this(requestId, taskType, provider, model, status, result, inputTokens, outputTokens, totalTokens,
                latencyMs, failureCategory, failureCode, providerRequestId, startedAt, completedAt,
                warnings != null ? warnings : Collections.emptyList(), null, null);
    }

    public static <T> AiTaskResult<T> success(
            UUID requestId,
            AiTaskType taskType,
            AiProviderType provider,
            String model,
            T result,
            Integer inputTokens,
            Integer outputTokens,
            Integer totalTokens,
            Integer cachedInputTokens,
            Long latencyMs,
            String providerRequestId,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt,
            List<String> warnings
    ) {
        return new AiTaskResult<>(
                requestId,
                taskType,
                provider,
                model,
                AiRequestStatus.COMPLETED,
                result,
                inputTokens,
                outputTokens,
                totalTokens,
                latencyMs,
                null,
                null,
                providerRequestId,
                startedAt,
                completedAt,
                warnings != null ? warnings : Collections.emptyList(),
                cachedInputTokens,
                null
        );
    }

    public static <T> AiTaskResult<T> success(
            UUID requestId,
            AiTaskType taskType,
            AiProviderType provider,
            String model,
            T result,
            Integer inputTokens,
            Integer outputTokens,
            Integer totalTokens,
            Long latencyMs,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt
    ) {
        return success(
                requestId,
                taskType,
                provider,
                model,
                result,
                inputTokens,
                outputTokens,
                totalTokens,
                null,
                latencyMs,
                null,
                startedAt,
                completedAt,
                Collections.emptyList()
        );
    }

    public static <T> AiTaskResult<T> failure(
            UUID requestId,
            AiTaskType taskType,
            AiProviderType provider,
            String model,
            String failureCategory,
            String failureCode,
            Long latencyMs,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt,
            List<String> warnings,
            Throwable cause
    ) {
        return new AiTaskResult<>(
                requestId,
                taskType,
                provider,
                model,
                AiRequestStatus.FAILED,
                null,
                null,
                null,
                null,
                latencyMs,
                failureCategory,
                failureCode,
                null,
                startedAt,
                completedAt,
                warnings != null ? warnings : Collections.emptyList(),
                null,
                cause
        );
    }

    public static <T> AiTaskResult<T> failure(
            UUID requestId,
            AiTaskType taskType,
            AiProviderType provider,
            String model,
            String failureCategory,
            String failureCode,
            Long latencyMs,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt,
            List<String> warnings
    ) {
        return failure(
                requestId,
                taskType,
                provider,
                model,
                failureCategory,
                failureCode,
                latencyMs,
                startedAt,
                completedAt,
                warnings,
                null
        );
    }

    public static <T> AiTaskResult<T> failure(
            UUID requestId,
            AiTaskType taskType,
            AiProviderType provider,
            String model,
            String failureCategory,
            String failureCode,
            Long latencyMs,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt
    ) {
        return failure(
                requestId,
                taskType,
                provider,
                model,
                failureCategory,
                failureCode,
                latencyMs,
                startedAt,
                completedAt,
                failureCode != null ? List.of(failureCode) : Collections.emptyList(),
                null
        );
    }

    public static <T> AiTaskResult<T> unavailable(
            UUID requestId,
            AiTaskType taskType,
            String reason
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        return new AiTaskResult<>(
                requestId,
                taskType,
                AiProviderType.NONE,
                "none",
                AiRequestStatus.CAPABILITY_UNAVAILABLE,
                null,
                null,
                null,
                null,
                0L,
                "CAPABILITY_UNAVAILABLE",
                reason,
                null,
                now,
                now,
                reason != null ? List.of(reason) : Collections.emptyList(),
                null,
                null
        );
    }
}
