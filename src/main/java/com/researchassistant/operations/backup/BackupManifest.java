package com.researchassistant.operations.backup;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BackupManifest(
        UUID backupRunId,
        @JsonSerialize(using = ToStringSerializer.class)
        OffsetDateTime createdAt,
        String applicationVersion,
        String flywaySchemaVersion,
        String databaseBackupReference,
        String databaseChecksum,
        String objectStorageBackupReference,
        String objectStorageChecksum,
        Long objectCount,
        String backupPolicyName,
        boolean encrypted,
        BackupRunStatus verificationStatus
) {
}
