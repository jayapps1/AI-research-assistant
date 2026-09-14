package com.researchassistant.operations.backup;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "backup_policies")
public class BackupPolicy {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BackupPolicyStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BackupFrequency frequency;

    @Column(name = "retention_daily", nullable = false)
    private Integer retentionDaily;

    @Column(name = "retention_weekly", nullable = false)
    private Integer retentionWeekly;

    @Column(name = "retention_monthly", nullable = false)
    private Integer retentionMonthly;

    @Column(name = "database_backup_enabled", nullable = false)
    private boolean databaseBackupEnabled;

    @Column(name = "object_storage_backup_enabled", nullable = false)
    private boolean objectStorageBackupEnabled;

    @Column(name = "verification_required", nullable = false)
    private boolean verificationRequired;

    @Column(name = "target_rpo_minutes")
    private Integer targetRpoMinutes;

    @Column(name = "target_rto_minutes")
    private Integer targetRtoMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BackupPolicyStatus getStatus() { return status; }
    public void setStatus(BackupPolicyStatus status) { this.status = status; }
    public BackupFrequency getFrequency() { return frequency; }
    public void setFrequency(BackupFrequency frequency) { this.frequency = frequency; }
    public Integer getRetentionDaily() { return retentionDaily; }
    public void setRetentionDaily(Integer retentionDaily) { this.retentionDaily = retentionDaily; }
    public Integer getRetentionWeekly() { return retentionWeekly; }
    public void setRetentionWeekly(Integer retentionWeekly) { this.retentionWeekly = retentionWeekly; }
    public Integer getRetentionMonthly() { return retentionMonthly; }
    public void setRetentionMonthly(Integer retentionMonthly) { this.retentionMonthly = retentionMonthly; }
    public boolean isDatabaseBackupEnabled() { return databaseBackupEnabled; }
    public void setDatabaseBackupEnabled(boolean databaseBackupEnabled) { this.databaseBackupEnabled = databaseBackupEnabled; }
    public boolean isObjectStorageBackupEnabled() { return objectStorageBackupEnabled; }
    public void setObjectStorageBackupEnabled(boolean objectStorageBackupEnabled) { this.objectStorageBackupEnabled = objectStorageBackupEnabled; }
    public boolean isVerificationRequired() { return verificationRequired; }
    public void setVerificationRequired(boolean verificationRequired) { this.verificationRequired = verificationRequired; }
    public Integer getTargetRpoMinutes() { return targetRpoMinutes; }
    public void setTargetRpoMinutes(Integer targetRpoMinutes) { this.targetRpoMinutes = targetRpoMinutes; }
    public Integer getTargetRtoMinutes() { return targetRtoMinutes; }
    public void setTargetRtoMinutes(Integer targetRtoMinutes) { this.targetRtoMinutes = targetRtoMinutes; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
