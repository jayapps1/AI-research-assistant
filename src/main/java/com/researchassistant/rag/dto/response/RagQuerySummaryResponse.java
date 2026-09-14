package com.researchassistant.rag.dto.response;

import com.researchassistant.rag.entity.RagQueryStatus;
import com.researchassistant.rag.scope.RetrievalScopeType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RagQuerySummaryResponse(
        UUID id,
        String question,
        RetrievalScopeType scopeType,
        RagQueryStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime completedAt
) {
}
