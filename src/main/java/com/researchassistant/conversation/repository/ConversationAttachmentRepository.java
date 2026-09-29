package com.researchassistant.conversation.repository;

import com.researchassistant.conversation.entity.ConversationAttachment;
import com.researchassistant.conversation.entity.ConversationAttachmentStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationAttachmentRepository extends JpaRepository<ConversationAttachment, UUID> {

    List<ConversationAttachment> findAllByConversationIdAndStatusOrderByCreatedAtAsc(
            UUID conversationId,
            ConversationAttachmentStatus status
    );

    Optional<ConversationAttachment> findByIdAndConversationId(UUID id, UUID conversationId);

    List<ConversationAttachment> findAllByConversationId(UUID conversationId);
}
