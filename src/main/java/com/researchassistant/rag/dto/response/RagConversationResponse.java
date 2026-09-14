package com.researchassistant.rag.dto.response;

import com.researchassistant.rag.entity.RagConversationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RagConversationResponse(
        UUID id,
        UUID projectId,
        UUID createdBy,
        String title,
        RagConversationStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
