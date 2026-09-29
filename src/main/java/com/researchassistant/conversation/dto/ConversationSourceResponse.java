package com.researchassistant.conversation.dto;

import com.researchassistant.conversation.entity.ConversationSourceType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ConversationSourceResponse(
        UUID id,
        ConversationSourceType sourceType,
        UUID documentId,
        String documentCode,
        UUID documentVersionId,
        Integer versionNumber,
        UUID projectReferenceId,
        String title,
        String url,
        String provider,
        List<String> authors,
        OffsetDateTime publishedAt,
        OffsetDateTime retrievedAt,
        Integer sourceOrdinal,
        Map<String, Object> metadata
) {
}
