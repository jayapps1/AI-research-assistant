package com.researchassistant.operations.backup;

import com.researchassistant.security.audit.SecurityAuditEventType;
import com.researchassistant.security.audit.SecurityAuditService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BackupVerificationService {

    private final BackupRunRepository runRepository;
    private final BackupArtifactRepository artifactRepository;
    private final DatabaseBackupProvider databaseBackupProvider;
    private final ObjectStorageBackupProvider objectStorageBackupProvider;
    private final FlywaySchemaVersionService schemaVersionService;
    private final SecurityAuditService auditService;

    public BackupVerificationService(
            BackupRunRepository runRepository,
            BackupArtifactRepository artifactRepository,
            DatabaseBackupProvider databaseBackupProvider,
            ObjectStorageBackupProvider objectStorageBackupProvider,
            FlywaySchemaVersionService schemaVersionService,
            SecurityAuditService auditService
    ) {
        this.runRepository = runRepository;
        this.artifactRepository = artifactRepository;
        this.databaseBackupProvider = databaseBackupProvider;
        this.objectStorageBackupProvider = objectStorageBackupProvider;
        this.schemaVersionService = schemaVersionService;
        this.auditService = auditService;
    }

    @Transactional
    public BackupVerificationOutcome verifyRun(
            UUID backupRunId,
            UUID actorUserId
    ) {
        BackupRun run = runRepository.findById(backupRunId)
                .orElseThrow(() -> new IllegalArgumentException("Backup run not found."));

        if (run.getManifestStorageReference() == null
                || run.getManifestStorageReference().isBlank()) {
            return fail(run, actorUserId, "MANIFEST_MISSING",
                    "Backup manifest reference is missing.");
        }
        if (run.getDatabaseSchemaVersion() == null
                || !run.getDatabaseSchemaVersion()
                .equals(schemaVersionService.currentVersion())) {
            return fail(run, actorUserId, "SCHEMA_VERSION_MISMATCH",
                    "Backup schema version does not match current Flyway state.");
        }

        for (BackupArtifact artifact
                : artifactRepository.findAllByBackupRunId(backupRunId)) {
            BackupArtifactDescriptor descriptor =
                    new BackupArtifactDescriptor(
                            artifact.getType(),
                            artifact.getStorageReference(),
                            artifact.getSizeBytes(),
                            artifact.getSha256Checksum(),
                            null
                    );
            BackupVerificationOutcome outcome =
                    artifact.getType() == BackupArtifactType.POSTGRESQL_DUMP
                            ? databaseBackupProvider.verifyBackupArtifact(descriptor)
                            : objectStorageBackupProvider.verify(descriptor);
            if (!outcome.valid()) {
                artifact.setStatus(BackupArtifactStatus.CORRUPT);
                return fail(run, actorUserId, outcome.code(), outcome.message());
            }
        }

        run.setStatus(BackupRunStatus.VERIFIED);
        auditService.record(actorUserId, SecurityAuditEventType.BACKUP_VERIFIED);
        return BackupVerificationOutcome.valid("Backup metadata and artifacts verified.");
    }

    private BackupVerificationOutcome fail(
            BackupRun run,
            UUID actorUserId,
            String code,
            String message
    ) {
        run.setStatus(BackupRunStatus.VERIFICATION_FAILED);
        run.setErrorCode(code);
        run.setErrorMessage(message);
        auditService.record(
                actorUserId,
                SecurityAuditEventType.BACKUP_VERIFICATION_FAILED
        );
        return BackupVerificationOutcome.invalid(code, message);
    }
}
