package com.researchassistant.operations.backup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BackupRunRepository extends JpaRepository<BackupRun, UUID> {

    Optional<BackupRun> findFirstByStatusInOrderByCompletedAtDesc(
            Collection<BackupRunStatus> statuses
    );

    Optional<BackupRun> findFirstByStatusOrderByCompletedAtDesc(
            BackupRunStatus status
    );

    List<BackupRun> findAllByCompletedAtBeforeAndStatusIn(
            OffsetDateTime completedBefore,
            Collection<BackupRunStatus> statuses
    );
}
