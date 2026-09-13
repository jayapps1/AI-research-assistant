package com.researchassistant.document.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Durable queue record for document-version processing.
 *
 * <p>The first implementation queues INGESTION work only. It does
 * not fake text extraction, embeddings or RAG completion; future
 * workers will advance these jobs and update the owning version.</p>
 */
@Entity
@Table(
        name = "document_processing_jobs",
        indexes = {
                @Index(name = "idx_document_processing_jobs_version_id", columnList = "document_version_id"),
                @Index(name = "idx_document_processing_jobs_status", columnList = "status"),
                @Index(name = "idx_document_processing_jobs_type_status", columnList = "type,status"),
                @Index(name = "idx_document_processing_jobs_queued_at", columnList = "queued_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class DocumentProcessingJob {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_version_id", nullable = false)
    private DocumentVersion documentVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private DocumentProcessingJobType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private DocumentProcessingStatus status;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber = 1;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "queued_at", nullable = false)
    private OffsetDateTime queuedAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "failed_at")
    private OffsetDateTime failedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (queuedAt == null) {
            queuedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = DocumentProcessingStatus.QUEUED;
        }
        if (attemptNumber < 1) {
            attemptNumber = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
