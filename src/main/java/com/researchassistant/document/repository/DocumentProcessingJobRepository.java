package com.researchassistant.document.repository;

import com.researchassistant.document.entity.DocumentProcessingJob;
import com.researchassistant.document.entity.DocumentProcessingJobType;
import com.researchassistant.document.entity.DocumentProcessingStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
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

    @Query(
            value = """
                    select *
                    from document_processing_jobs
                    where status = 'QUEUED'
                      and type = cast(:type as varchar)
                    order by queued_at
                    for update skip locked
                    limit 1
                    """,
            nativeQuery = true
    )
    Optional<DocumentProcessingJob> findNextQueuedForUpdateSkipLocked(
            @Param("type") String type
    );
}
