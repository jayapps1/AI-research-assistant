package com.researchassistant.document.repository;

import com.researchassistant.document.entity.DocumentChunkEmbedding;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentChunkEmbeddingRepository
        extends JpaRepository<DocumentChunkEmbedding, UUID> {

    Optional<DocumentChunkEmbedding> findByChunkIdAndProviderAndModel(
            UUID chunkId,
            String provider,
            String model
    );

    java.util.List<DocumentChunkEmbedding> findAllByChunkId(UUID chunkId);

    void deleteByChunkDocumentVersionId(UUID versionId);
}
