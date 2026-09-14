package com.researchassistant.document.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "document_text_extractions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_document_text_extractions_version",
                columnNames = "document_version_id"
        ),
        indexes = {
                @Index(name = "idx_document_text_extractions_version_id", columnList = "document_version_id"),
                @Index(name = "idx_document_text_extractions_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class DocumentTextExtraction {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_version_id", nullable = false)
    private DocumentVersion documentVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private TextExtractionStatus status = TextExtractionStatus.PENDING;

    @Column(name = "extractor", nullable = false, length = 100)
    private String extractor;

    @Column(name = "extractor_version", length = 100)
    private String extractorVersion;

    @Column(name = "page_count", nullable = false)
    private int pageCount;

    @Column(name = "character_count", nullable = false)
    private long characterCount;

    @Column(name = "language", length = 50)
    private String language;

    @Column(name = "content_checksum_sha256", length = 64)
    private String contentChecksumSha256;

    @Column(name = "ocr_required", nullable = false)
    private boolean ocrRequired;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message", length = 2000)
    private String failureMessage;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

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
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = TextExtractionStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
