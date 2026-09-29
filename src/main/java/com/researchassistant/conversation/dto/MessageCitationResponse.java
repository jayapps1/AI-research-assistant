package com.researchassistant.conversation.dto;

import com.researchassistant.conversation.entity.ConversationSourceType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MessageCitationResponse(
        UUID id,
        int number,
        UUID sourceId,
        ConversationSourceType sourceType,
        String title,
        String url,
        String provider,
        List<String> authors,
        OffsetDateTime publishedAt,
        OffsetDateTime retrievedAt,
        UUID documentId,
        String documentCode,
        String documentTitle,
        UUID documentVersionId,
        Integer versionNumber,
        UUID projectReferenceId,
        Integer pageNumber,
        Integer chunkNumber,
        String supportingExcerpt,
        String formattedCitation,
        Map<String, Object> metadata
) {
}
