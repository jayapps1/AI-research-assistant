package com.researchassistant.conversation.repository;

import com.researchassistant.conversation.entity.ConversationMessage;
import com.researchassistant.conversation.entity.ConversationMessageRole;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {

    List<ConversationMessage> findAllByConversationIdAndRoleNotOrderBySequenceNumberAsc(
            UUID conversationId,
            ConversationMessageRole excludedRole
    );

    List<ConversationMessage> findAllByConversationIdAndRoleNotOrderBySequenceNumberDesc(
            UUID conversationId,
            ConversationMessageRole excludedRole,
            Pageable pageable
    );

    Optional<ConversationMessage> findByIdAndConversationIdAndRole(UUID id, UUID conversationId, ConversationMessageRole role);

    @Query("""
            SELECT COALESCE(MAX(m.sequenceNumber), 0)
            FROM ConversationMessage m
            WHERE m.conversation.id = :conversationId
            """)
    int maxSequenceForConversation(@Param("conversationId") UUID conversationId);
}
