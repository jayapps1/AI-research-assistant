package com.researchassistant.operations.restore;

import com.researchassistant.operations.backup.BackupRun;
import com.researchassistant.operations.backup.BackupRunRepository;
import com.researchassistant.operations.backup.BackupRunStatus;
import com.researchassistant.operations.backup.BackupVerificationOutcome;
import com.researchassistant.operations.backup.FlywaySchemaVersionService;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RestoreVerificationService {

    private final BackupRunRepository backupRunRepository;
    private final FlywaySchemaVersionService schemaVersionService;

    public RestoreVerificationService(
            BackupRunRepository backupRunRepository,
            FlywaySchemaVersionService schemaVersionService
    ) {
        this.backupRunRepository = backupRunRepository;
        this.schemaVersionService = schemaVersionService;
    }

    public BackupVerificationOutcome verifyBackupCanBeRestored(
            UUID backupRunId,
            RestoreTargetEnvironment targetEnvironment
    ) {
        if (targetEnvironment == RestoreTargetEnvironment.PRODUCTION) {
            return BackupVerificationOutcome.invalid(
                    "PRODUCTION_RESTORE_REQUIRES_RUNBOOK",
                    "Production restore must use the operational runbook and explicit approval."
            );
        }
        BackupRun run = backupRunRepository.findById(backupRunId)
                .orElseThrow(() -> new IllegalArgumentException("Backup run not found."));
        if (run.getStatus() != BackupRunStatus.VERIFIED) {
            return BackupVerificationOutcome.invalid(
                    "BACKUP_NOT_VERIFIED",
                    "Only verified backups should be restored."
            );
        }
        if (run.getDatabaseSchemaVersion() == null
                || !run.getDatabaseSchemaVersion()
                .equals(schemaVersionService.currentVersion())) {
            return BackupVerificationOutcome.invalid(
                    "SCHEMA_VERSION_MISMATCH",
                    "Backup Flyway version does not match this application."
            );
        }
        return BackupVerificationOutcome.valid("Backup is eligible for restore drill.");
    }
}
