package com.researchassistant.reference.entity;

import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reference_import_jobs")
@Getter @Setter @NoArgsConstructor
public class ReferenceImportJob {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false) private ResearchProject project;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private ReferenceImportFormat format;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private ReferenceImportStatus status = ReferenceImportStatus.UPLOADED;
    @Column(name = "original_filename", nullable = false, length = 500) private String originalFilename;
    @Column(name = "storage_key", length = 1000) private String storageKey;
    @Column(name = "file_size_bytes", nullable = false) private long fileSizeBytes;
    @Column(name = "checksum_sha256", nullable = false, length = 64) private String checksumSha256;
    @Column(name = "detected_entries") private Integer detectedEntries;
    @Column(name = "imported_entries") private Integer importedEntries;
    @Column(name = "skipped_entries") private Integer skippedEntries;
    @Column(name = "duplicate_entries") private Integer duplicateEntries;
    @Column(name = "failed_entries") private Integer failedEntries;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "uploaded_by", nullable = false) private User uploadedBy;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "completed_at") private OffsetDateTime completedAt;
    @Column(name = "error_code", length = 100) private String errorCode;
    @Column(name = "error_message", columnDefinition = "TEXT") private String errorMessage;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = OffsetDateTime.now(); if (status == null) status = ReferenceImportStatus.UPLOADED; }
}
