package com.researchassistant.operations.backup;

public interface ObjectStorageBackupProvider {

    BackupArtifactDescriptor createSnapshotOrArchive(BackupRun backupRun);

    BackupVerificationOutcome verify(BackupArtifactDescriptor artifact);

    BackupProviderCapabilities describeCapabilities();
}
