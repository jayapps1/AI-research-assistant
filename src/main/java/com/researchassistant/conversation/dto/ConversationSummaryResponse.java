package com.researchassistant.conversation.dto;

import com.researchassistant.conversation.entity.ConversationStatus;
import com.researchassistant.conversation.entity.ConversationType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConversationSummaryResponse(
        UUID id,
        UUID userId,
        UUID workspaceId,
        String workspaceName,
        UUID projectId,
        String projectTitle,
        ConversationType type,
        ConversationStatus status,
        String title,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime lastMessageAt,
        OffsetDateTime archivedAt,
        OffsetDateTime deletedAt
) {
}
