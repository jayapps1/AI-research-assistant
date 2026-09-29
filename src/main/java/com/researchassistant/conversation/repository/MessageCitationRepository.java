package com.researchassistant.conversation.repository;

import com.researchassistant.conversation.entity.MessageCitation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageCitationRepository extends JpaRepository<MessageCitation, UUID> {

    @EntityGraph(attributePaths = {
            "source",
            "source.document",
            "source.documentVersion",
            "source.projectReference",
            "source.projectReference.reference"
    })
    List<MessageCitation> findAllByMessageIdOrderByCitationOrdinalAsc(UUID messageId);

    @EntityGraph(attributePaths = {
            "source",
            "source.document",
            "source.documentVersion",
            "source.projectReference",
            "source.projectReference.reference"
    })
    List<MessageCitation> findAllByMessageConversationIdOrderByMessageSequenceNumberAscCitationOrdinalAsc(UUID conversationId);
}
