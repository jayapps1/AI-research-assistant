package com.researchassistant.operations.backup;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "backup_runs")
public class BackupRun {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private BackupPolicy policy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BackupType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BackupRunStatus status;

    @Column(name = "initiated_by", nullable = false)
    private String initiatedBy;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "application_version")
    private String applicationVersion;

    @Column(name = "database_schema_version")
    private String databaseSchemaVersion;

    @Column(name = "database_backup_reference")
    private String databaseBackupReference;

    @Column(name = "object_storage_backup_reference")
    private String objectStorageBackupReference;

    @Column(name = "manifest_storage_reference")
    private String manifestStorageReference;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "duration_ms")
    private Long durationMs;

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
    public BackupPolicy getPolicy() { return policy; }
    public void setPolicy(BackupPolicy policy) { this.policy = policy; }
    public BackupType getType() { return type; }
    public void setType(BackupType type) { this.type = type; }
    public BackupRunStatus getStatus() { return status; }
    public void setStatus(BackupRunStatus status) { this.status = status; }
    public String getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(String initiatedBy) { this.initiatedBy = initiatedBy; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
    public String getApplicationVersion() { return applicationVersion; }
    public void setApplicationVersion(String applicationVersion) { this.applicationVersion = applicationVersion; }
    public String getDatabaseSchemaVersion() { return databaseSchemaVersion; }
    public void setDatabaseSchemaVersion(String databaseSchemaVersion) { this.databaseSchemaVersion = databaseSchemaVersion; }
    public String getDatabaseBackupReference() { return databaseBackupReference; }
    public void setDatabaseBackupReference(String databaseBackupReference) { this.databaseBackupReference = databaseBackupReference; }
    public String getObjectStorageBackupReference() { return objectStorageBackupReference; }
    public void setObjectStorageBackupReference(String objectStorageBackupReference) { this.objectStorageBackupReference = objectStorageBackupReference; }
    public String getManifestStorageReference() { return manifestStorageReference; }
    public void setManifestStorageReference(String manifestStorageReference) { this.manifestStorageReference = manifestStorageReference; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
