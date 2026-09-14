package com.researchassistant.dataset.model;

import com.researchassistant.identity.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dataset_import_jobs", indexes = @Index(name = "idx_dataset_import_jobs_dataset", columnList = "dataset_id"))
@Getter @Setter @NoArgsConstructor
public class DatasetImportJob {
    public enum Format { CSV, XLSX }
    public enum Status { UPLOADED, PROFILING, AWAITING_MAPPING, VALIDATING, IMPORTING, COMPLETED, COMPLETED_WITH_ERRORS, FAILED, CANCELLED }
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "dataset_id", nullable = false)
    private ResearchDataset dataset;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Format format;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private Status status = Status.UPLOADED;
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;
    @Column(name = "storage_key", length = 500)
    private String storageKey;
    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;
    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;
    @Column(name = "total_rows")
    private Integer totalRows;
    @Column(name = "imported_rows")
    private Integer importedRows;
    @Column(name = "rejected_rows")
    private Integer rejectedRows;
    @Column(name = "error_code", length = 100)
    private String errorCode;
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "started_at")
    private OffsetDateTime startedAt;
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
    @Version
    private Long version;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = OffsetDateTime.now(); if (status == null) status = Status.UPLOADED; }
}
