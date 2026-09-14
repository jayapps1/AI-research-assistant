package com.researchassistant.operations.backup;

import org.springframework.stereotype.Component;

@Component
public class ExternalManagedDatabaseBackupProvider
        implements DatabaseBackupProvider {

    @Override
    public BackupArtifactDescriptor createBackup(BackupRun backupRun) {
        String reference = backupRun.getDatabaseBackupReference();
        if (reference == null || reference.isBlank()) {
            reference = "external-managed://pending/" + backupRun.getId();
        }
        return new BackupArtifactDescriptor(
                BackupArtifactType.POSTGRESQL_DUMP,
                reference,
                null,
                null,
                null
        );
    }

    @Override
    public BackupVerificationOutcome verifyBackupArtifact(
            BackupArtifactDescriptor artifact
    ) {
        if (artifact.storageReference() == null
                || artifact.storageReference().isBlank()) {
            return BackupVerificationOutcome.invalid(
                    "DATABASE_BACKUP_REFERENCE_MISSING",
                    "Database backup reference is missing."
            );
        }
        return BackupVerificationOutcome.valid(
                "Database backup is externally managed; verify with infrastructure backup tooling."
        );
    }

    @Override
    public BackupProviderCapabilities describeCapabilities() {
        return new BackupProviderCapabilities(
                "external-managed-postgresql",
                false,
                false,
                false,
                false,
                "Records managed backup references; pg_dump, pgBackRest, WAL/PITR are operational concerns."
        );
    }
}
