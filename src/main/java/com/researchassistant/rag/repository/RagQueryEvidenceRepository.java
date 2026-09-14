package com.researchassistant.rag.repository;

import com.researchassistant.rag.entity.RagQueryEvidence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RagQueryEvidenceRepository
        extends JpaRepository<RagQueryEvidence, UUID> {

    List<RagQueryEvidence> findAllByQueryIdOrderByEvidenceOrdinalAsc(UUID queryId);

    Optional<RagQueryEvidence> findByQueryIdAndEvidenceOrdinal(
            UUID queryId,
            int evidenceOrdinal
    );

    @EntityGraph(attributePaths = {
            "query",
            "query.conversation",
            "query.conversation.project",
            "query.conversation.project.workspace",
            "chunk",
            "chunk.page",
            "chunk.documentVersion",
            "chunk.documentVersion.document"
    })
    List<RagQueryEvidence> findWithTraceByQueryIdOrderByEvidenceOrdinalAsc(UUID queryId);
}
