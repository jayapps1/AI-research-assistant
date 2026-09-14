package com.researchassistant.document.repository;

import com.researchassistant.document.entity.DocumentChunk;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    List<DocumentChunk> findAllByDocumentVersionIdOrderByChunkNumber(UUID versionId);

    void deleteByDocumentVersionId(UUID versionId);
}
