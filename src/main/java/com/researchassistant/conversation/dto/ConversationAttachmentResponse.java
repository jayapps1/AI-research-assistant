package com.researchassistant.conversation.dto;

import com.researchassistant.conversation.entity.ConversationAttachment;
import com.researchassistant.conversation.entity.ConversationAttachmentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConversationAttachmentResponse(
        UUID id,
        UUID conversationId,
        UUID storageObjectId,
        String originalFilename,
        String mediaType,
        long sizeBytes,
        ConversationAttachmentStatus status,
        OffsetDateTime createdAt
) {
    public static ConversationAttachmentResponse from(ConversationAttachment attachment) {
        return new ConversationAttachmentResponse(
                attachment.getId(),
                attachment.getConversation().getId(),
                attachment.getStorageObject().getId(),
                attachment.getOriginalFilename(),
                attachment.getMediaType(),
                attachment.getSizeBytes(),
                attachment.getStatus(),
                attachment.getCreatedAt()
        );
    }
}
