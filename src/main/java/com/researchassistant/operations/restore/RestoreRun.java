package com.researchassistant.operations.restore;

import com.researchassistant.operations.backup.BackupRun;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "restore_runs")
public class RestoreRun {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "backup_run_id", nullable = false)
    private BackupRun backupRun;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RestoreRunStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_environment", nullable = false)
    private RestoreTargetEnvironment targetEnvironment;

    @Column(name = "target_database_reference")
    private String targetDatabaseReference;

    @Column(name = "target_storage_reference")
    private String targetStorageReference;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "initiated_by", nullable = false)
    private String initiatedBy;

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
    public BackupRun getBackupRun() { return backupRun; }
    public void setBackupRun(BackupRun backupRun) { this.backupRun = backupRun; }
    public RestoreRunStatus getStatus() { return status; }
    public void setStatus(RestoreRunStatus status) { this.status = status; }
    public RestoreTargetEnvironment getTargetEnvironment() { return targetEnvironment; }
    public void setTargetEnvironment(RestoreTargetEnvironment targetEnvironment) { this.targetEnvironment = targetEnvironment; }
    public String getTargetDatabaseReference() { return targetDatabaseReference; }
    public void setTargetDatabaseReference(String targetDatabaseReference) { this.targetDatabaseReference = targetDatabaseReference; }
    public String getTargetStorageReference() { return targetStorageReference; }
    public void setTargetStorageReference(String targetStorageReference) { this.targetStorageReference = targetStorageReference; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
    public String getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(String initiatedBy) { this.initiatedBy = initiatedBy; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
