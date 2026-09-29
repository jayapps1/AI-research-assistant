package com.researchassistant.conversation.dto;

import com.researchassistant.conversation.entity.ConversationMessageRole;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ConversationMessageResponse(
        UUID id,
        ConversationMessageRole role,
        String content,
        String structuredContent,
        int sequenceNumber,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<MessageCitationResponse> citations,
        List<ConversationSourceResponse> sources
) {
}
