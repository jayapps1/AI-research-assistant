package com.researchassistant.rag.repository;

import com.researchassistant.rag.entity.RagConversation;
import com.researchassistant.rag.entity.RagConversationStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RagConversationRepository
        extends JpaRepository<RagConversation, UUID> {

    List<RagConversation> findAllByProjectIdAndCreatedByIdAndStatusOrderByUpdatedAtDesc(
            UUID projectId,
            UUID createdById,
            RagConversationStatus status
    );

    Optional<RagConversation> findByIdAndCreatedById(UUID id, UUID createdById);
}
