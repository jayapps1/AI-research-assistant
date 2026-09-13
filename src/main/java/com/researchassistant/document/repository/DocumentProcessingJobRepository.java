package com.researchassistant.document.repository;

import com.researchassistant.document.entity.DocumentProcessingJob;
import com.researchassistant.document.entity.DocumentProcessingJobType;
import com.researchassistant.document.entity.DocumentProcessingStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentProcessingJobRepository
        extends JpaRepository<DocumentProcessingJob, UUID> {

    List<DocumentProcessingJob> findAllByDocumentVersionDocumentIdOrderByQueuedAtDesc(
            UUID documentId
    );

    long countByDocumentVersionIdAndType(
            UUID documentVersionId,
            DocumentProcessingJobType type
    );

    boolean existsByDocumentVersionIdAndTypeAndStatus(
            UUID documentVersionId,
            DocumentProcessingJobType type,
            DocumentProcessingStatus status
    );
}
