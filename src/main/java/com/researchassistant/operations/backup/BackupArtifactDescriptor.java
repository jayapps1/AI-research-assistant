package com.researchassistant.operations.backup;

public record BackupArtifactDescriptor(
        BackupArtifactType type,
        String storageReference,
        Long sizeBytes,
        String sha256Checksum,
        Long objectCount
) {
}
