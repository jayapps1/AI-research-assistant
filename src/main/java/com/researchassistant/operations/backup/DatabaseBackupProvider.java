package com.researchassistant.operations.backup;

public interface DatabaseBackupProvider {

    BackupArtifactDescriptor createBackup(BackupRun backupRun);

    BackupVerificationOutcome verifyBackupArtifact(
            BackupArtifactDescriptor artifact
    );

    BackupProviderCapabilities describeCapabilities();
}
