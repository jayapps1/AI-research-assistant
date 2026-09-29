package com.researchassistant.conversation.repository;

import com.researchassistant.conversation.entity.ConversationRun;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRunRepository extends JpaRepository<ConversationRun, UUID> {

    Optional<ConversationRun> findFirstByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    Optional<ConversationRun> findFirstByConversationIdAndUserMessageIdOrderByCreatedAtDesc(
            UUID conversationId,
            UUID userMessageId
    );

    List<ConversationRun> findAllByConversationIdAndAssistantMessageIsNotNull(UUID conversationId);
}
