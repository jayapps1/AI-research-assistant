package com.researchassistant.rag.repository;

import com.researchassistant.rag.entity.RagQuery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RagQueryRepository extends JpaRepository<RagQuery, UUID> {

    Page<RagQuery> findAllByConversationIdOrderByCreatedAtAsc(
            UUID conversationId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "conversation",
            "conversation.project",
            "conversation.project.workspace",
            "createdBy"
    })
    Optional<RagQuery> findWithConversationById(UUID id);
}
