package com.researchassistant.document.repository;

import com.researchassistant.document.entity.DocumentVersion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentVersionRepository
        extends JpaRepository<DocumentVersion, UUID> {

    List<DocumentVersion> findAllByDocumentIdOrderByVersionNumberDesc(
            UUID documentId
    );

    Optional<DocumentVersion> findByDocumentIdAndVersionNumber(
            UUID documentId,
            int versionNumber
    );
}
