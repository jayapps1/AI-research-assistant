package com.researchassistant.document.entity;

import com.researchassistant.identity.entity.User;
import com.researchassistant.document.security.FileScanStatus;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Immutable metadata for one uploaded file revision.
 *
 * <p>Uploading a replacement creates a new row. Existing version
 * metadata remains available for provenance, citation traceability
 * and future RAG evidence reconstruction.</p>
 */
@Entity
@Table(
        name = "document_versions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_document_versions_document_number",
                        columnNames = {"document_id", "version_number"}
                )
        },
        indexes = {
                @Index(name = "idx_document_versions_document_id", columnList = "document_id"),
                @Index(name = "idx_document_versions_document_status", columnList = "document_id,status"),
                @Index(name = "idx_document_versions_uploaded_by", columnList = "uploaded_by"),
                @Index(name = "idx_document_versions_checksum", columnList = "checksum_sha256")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class DocumentVersion {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;

    @Column(name = "storage_key", nullable = false, length = 1000)
    private String storageKey;

    @Column(name = "mime_type", nullable = false, length = 255)
    private String mimeType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_status", nullable = false, length = 30)
    private FileScanStatus scanStatus = FileScanStatus.NOT_SCANNED;

    @Column(name = "quarantined", nullable = false)
    private boolean quarantined;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private DocumentVersionStatus status = DocumentVersionStatus.UPLOADING;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (uploadedAt == null) {
            uploadedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (status == null) {
            status = DocumentVersionStatus.UPLOADING;
        }
        if (scanStatus == null) {
            scanStatus = FileScanStatus.NOT_SCANNED;
        }
    }
}
