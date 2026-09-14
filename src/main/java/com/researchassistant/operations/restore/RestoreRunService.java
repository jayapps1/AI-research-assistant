package com.researchassistant.operations.restore;

import com.researchassistant.operations.backup.BackupRun;
import com.researchassistant.operations.backup.BackupRunRepository;
import com.researchassistant.operations.backup.BackupVerificationOutcome;
import com.researchassistant.security.audit.SecurityAuditEventType;
import com.researchassistant.security.audit.SecurityAuditService;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class RestoreRunService {

    private final RestoreRunRepository restoreRunRepository;
    private final BackupRunRepository backupRunRepository;
    private final RestoreVerificationService verificationService;
    private final SecurityAuditService auditService;
    private final MeterRegistry meterRegistry;

    public RestoreRunService(
            RestoreRunRepository restoreRunRepository,
            BackupRunRepository backupRunRepository,
            RestoreVerificationService verificationService,
            SecurityAuditService auditService,
            MeterRegistry meterRegistry
    ) {
        this.restoreRunRepository = restoreRunRepository;
        this.backupRunRepository = backupRunRepository;
        this.verificationService = verificationService;
        this.auditService = auditService;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public RestoreRun requestRestoreDrill(
            UUID backupRunId,
            RestoreTargetEnvironment targetEnvironment,
            String initiatedBy,
            UUID actorUserId
    ) {
        BackupVerificationOutcome outcome =
                verificationService.verifyBackupCanBeRestored(
                        backupRunId,
                        targetEnvironment
                );
        BackupRun backupRun = backupRunRepository.findById(backupRunId)
                .orElseThrow(() -> new IllegalArgumentException("Backup run not found."));

        RestoreRun restoreRun = new RestoreRun();
        restoreRun.setBackupRun(backupRun);
        restoreRun.setTargetEnvironment(targetEnvironment);
        restoreRun.setStartedAt(OffsetDateTime.now());
        restoreRun.setInitiatedBy(initiatedBy);

        if (outcome.valid()) {
            restoreRun.setStatus(RestoreRunStatus.REQUESTED);
            auditService.record(actorUserId, SecurityAuditEventType.RESTORE_STARTED);
        } else {
            restoreRun.setStatus(RestoreRunStatus.FAILED);
            restoreRun.setErrorCode(outcome.code());
            restoreRun.setErrorMessage(outcome.message());
            restoreRun.setCompletedAt(OffsetDateTime.now());
            restoreRun.setDurationMs(0L);
            meterRegistry.counter("research.restore.failure.count").increment();
            auditService.record(actorUserId, SecurityAuditEventType.RESTORE_FAILED);
        }
        return restoreRunRepository.save(restoreRun);
    }

    @Transactional
    public void completeRestore(UUID restoreRunId, UUID actorUserId) {
        RestoreRun run = restoreRunRepository.findById(restoreRunId)
                .orElseThrow(() -> new IllegalArgumentException("Restore run not found."));
        run.setStatus(RestoreRunStatus.COMPLETED);
        OffsetDateTime completedAt = OffsetDateTime.now();
        run.setCompletedAt(completedAt);
        run.setDurationMs(Duration.between(run.getStartedAt(), completedAt).toMillis());
        meterRegistry.counter("research.restore.success.count").increment();
        auditService.record(actorUserId, SecurityAuditEventType.RESTORE_COMPLETED);
    }
}
