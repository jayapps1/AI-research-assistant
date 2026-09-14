package com.researchassistant.document.repository;

import com.researchassistant.document.entity.DocumentTextExtraction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentTextExtractionRepository
        extends JpaRepository<DocumentTextExtraction, UUID> {

    Optional<DocumentTextExtraction> findByDocumentVersionId(UUID versionId);

    void deleteByDocumentVersionId(UUID versionId);
}
