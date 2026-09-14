package com.researchassistant.operations.backup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BackupArtifactRepository
        extends JpaRepository<BackupArtifact, UUID> {

    List<BackupArtifact> findAllByBackupRunId(UUID backupRunId);
}
