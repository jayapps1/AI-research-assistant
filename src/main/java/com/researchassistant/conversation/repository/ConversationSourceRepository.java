package com.researchassistant.conversation.repository;

import com.researchassistant.conversation.entity.ConversationSource;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationSourceRepository extends JpaRepository<ConversationSource, UUID> {

    @EntityGraph(attributePaths = {
            "message",
            "document",
            "documentVersion",
            "projectReference",
            "projectReference.reference"
    })
    List<ConversationSource> findAllByMessageIdOrderBySourceOrdinalAscCreatedAtAsc(UUID messageId);

    Optional<ConversationSource> findByIdAndConversationUserId(UUID id, UUID userId);
}
