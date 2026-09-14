package com.researchassistant.operations.restore;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;

public interface RestoreRunRepository extends JpaRepository<RestoreRun, UUID> {

    boolean existsByBackupRunIdAndStatusIn(
            UUID backupRunId,
            Collection<RestoreRunStatus> statuses
    );
}
