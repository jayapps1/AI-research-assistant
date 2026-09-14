package com.researchassistant.operations.backup;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class BackupManifestService {

    public BackupManifest createManifest(
            BackupRun run,
            BackupArtifactDescriptor databaseArtifact,
            BackupArtifactDescriptor objectStorageArtifact
    ) {
        return new BackupManifest(
                run.getId(),
                OffsetDateTime.now(),
                run.getApplicationVersion(),
                run.getDatabaseSchemaVersion(),
                databaseArtifact == null
                        ? null
                        : databaseArtifact.storageReference(),
                databaseArtifact == null
                        ? null
                        : databaseArtifact.sha256Checksum(),
                objectStorageArtifact == null
                        ? null
                        : objectStorageArtifact.storageReference(),
                objectStorageArtifact == null
                        ? null
                        : objectStorageArtifact.sha256Checksum(),
                objectStorageArtifact == null
                        ? null
                        : objectStorageArtifact.objectCount(),
                run.getPolicy() == null ? null : run.getPolicy().getName(),
                true,
                run.getStatus()
        );
    }
}
