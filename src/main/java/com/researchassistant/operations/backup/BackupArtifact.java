package com.researchassistant.operations.backup;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "backup_artifacts")
public class BackupArtifact {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "backup_run_id", nullable = false)
    private BackupRun backupRun;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BackupArtifactType type;

    @Column(name = "storage_reference", nullable = false)
    private String storageReference;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "sha256_checksum", length = 64)
    private String sha256Checksum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BackupArtifactStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public BackupRun getBackupRun() { return backupRun; }
    public void setBackupRun(BackupRun backupRun) { this.backupRun = backupRun; }
    public BackupArtifactType getType() { return type; }
    public void setType(BackupArtifactType type) { this.type = type; }
    public String getStorageReference() { return storageReference; }
    public void setStorageReference(String storageReference) { this.storageReference = storageReference; }
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    public String getSha256Checksum() { return sha256Checksum; }
    public void setSha256Checksum(String sha256Checksum) { this.sha256Checksum = sha256Checksum; }
    public BackupArtifactStatus getStatus() { return status; }
    public void setStatus(BackupArtifactStatus status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
