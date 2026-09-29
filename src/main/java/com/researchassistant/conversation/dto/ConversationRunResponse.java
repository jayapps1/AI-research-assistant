package com.researchassistant.conversation.dto;

import com.researchassistant.conversation.entity.ConversationRunStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ConversationRunResponse(
        UUID id,
        UUID userMessageId,
        UUID assistantMessageId,
        UUID aiRequestId,
        UUID ragQueryId,
        String operationType,
        String searchScope,
        ConversationRunStatus status,
        String provider,
        String model,
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens,
        Integer cachedInputTokens,
        BigDecimal providerCost,
        BigDecimal platformCredits,
        String webProvider,
        Integer webResultCount,
        BigDecimal webProviderCost,
        String errorCode,
        String failureMessage,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt
) {
}
