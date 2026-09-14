package com.researchassistant.operations.backup;

import com.researchassistant.operations.restore.RestoreRunRepository;
import com.researchassistant.operations.restore.RestoreRunStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;

@Service
public class BackupRetentionService {

    private static final List<BackupRunStatus> RETAINABLE_STATUSES = List.of(
            BackupRunStatus.COMPLETED,
            BackupRunStatus.VERIFICATION_PENDING,
            BackupRunStatus.VERIFIED,
            BackupRunStatus.VERIFICATION_FAILED,
            BackupRunStatus.FAILED
    );

    private final BackupRunRepository runRepository;
    private final RestoreRunRepository restoreRunRepository;

    public BackupRetentionService(
            BackupRunRepository runRepository,
            RestoreRunRepository restoreRunRepository
    ) {
        this.runRepository = runRepository;
        this.restoreRunRepository = restoreRunRepository;
    }

    @Transactional(readOnly = true)
    public List<BackupRun> eligibleForMetadataRetention(
            BackupPolicy policy,
            OffsetDateTime now
    ) {
        OffsetDateTime cutoff = now.minusDays(
                Math.max(1, policy.getRetentionDaily())
        );
        List<BackupRun> candidates =
                runRepository.findAllByCompletedAtBeforeAndStatusIn(
                        cutoff,
                        RETAINABLE_STATUSES
                );
        boolean hasVerifiedBackup = runRepository
                .findFirstByStatusOrderByCompletedAtDesc(
                        BackupRunStatus.VERIFIED
                )
                .isPresent();
        return candidates.stream()
                .filter(run -> run.getStatus() != BackupRunStatus.VERIFIED
                        || hasVerifiedBackup)
                .filter(run -> !restoreRunRepository.existsByBackupRunIdAndStatusIn(
                        run.getId(),
                        EnumSet.of(
                                RestoreRunStatus.REQUESTED,
                                RestoreRunStatus.VALIDATING_BACKUP,
                                RestoreRunStatus.RESTORING_DATABASE,
                                RestoreRunStatus.RESTORING_OBJECT_STORAGE,
                                RestoreRunStatus.VERIFYING
                        )
                ))
                .toList();
    }
}
