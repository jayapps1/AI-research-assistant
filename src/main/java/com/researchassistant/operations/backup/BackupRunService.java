package com.researchassistant.operations.backup;

import com.researchassistant.security.audit.SecurityAuditEventType;
import com.researchassistant.security.audit.SecurityAuditService;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class BackupRunService {

    private final BackupRunRepository runRepository;
    private final BackupArtifactRepository artifactRepository;
    private final DatabaseBackupProvider databaseBackupProvider;
    private final ObjectStorageBackupProvider objectStorageBackupProvider;
    private final FlywaySchemaVersionService schemaVersionService;
    private final BackupManifestService manifestService;
    private final ChecksumService checksumService;
    private final SecurityAuditService auditService;
    private final MeterRegistry meterRegistry;
    private final String applicationVersion;

    public BackupRunService(
            BackupRunRepository runRepository,
            BackupArtifactRepository artifactRepository,
            DatabaseBackupProvider databaseBackupProvider,
            ObjectStorageBackupProvider objectStorageBackupProvider,
            FlywaySchemaVersionService schemaVersionService,
            BackupManifestService manifestService,
            ChecksumService checksumService,
            SecurityAuditService auditService,
            MeterRegistry meterRegistry,
            @Value("${spring.application.name:research-assistant-api}")
            String applicationVersion
    ) {
        this.runRepository = runRepository;
        this.artifactRepository = artifactRepository;
        this.databaseBackupProvider = databaseBackupProvider;
        this.objectStorageBackupProvider = objectStorageBackupProvider;
        this.schemaVersionService = schemaVersionService;
        this.manifestService = manifestService;
        this.checksumService = checksumService;
        this.auditService = auditService;
        this.meterRegistry = meterRegistry;
        this.applicationVersion = applicationVersion;
    }

    @Transactional
    public BackupRun startRun(
            BackupPolicy policy,
            BackupType type,
            String initiatedBy,
            UUID actorUserId
    ) {
        BackupRun run = new BackupRun();
        run.setPolicy(policy);
        run.setType(type);
        run.setStatus(BackupRunStatus.RUNNING);
        run.setInitiatedBy(initiatedBy);
        run.setStartedAt(OffsetDateTime.now());
        run.setApplicationVersion(applicationVersion);
        run.setDatabaseSchemaVersion(schemaVersionService.currentVersion());
        BackupRun saved = runRepository.save(run);
        auditService.record(actorUserId, SecurityAuditEventType.BACKUP_STARTED);
        return saved;
    }

    @Transactional
    public BackupManifest completeRun(UUID runId, UUID actorUserId) {
        BackupRun run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Backup run not found."));

        BackupArtifactDescriptor databaseArtifact =
                databaseBackupProvider.createBackup(run);
        BackupArtifactDescriptor objectStorageArtifact =
                objectStorageBackupProvider.createSnapshotOrArchive(run);

        run.setDatabaseBackupReference(databaseArtifact.storageReference());
        run.setObjectStorageBackupReference(
                objectStorageArtifact.storageReference()
        );
        run.setStatus(BackupRunStatus.VERIFICATION_PENDING);
        finishTiming(run);

        saveArtifact(run, databaseArtifact);
        saveArtifact(run, objectStorageArtifact);

        BackupManifest manifest = manifestService.createManifest(
                run,
                databaseArtifact,
                objectStorageArtifact
        );
        run.setManifestStorageReference(
                "backup-manifest://" + checksumService.sha256(manifest)
        );
        meterRegistry.counter("research.backup.success.count").increment();
        Timer.builder("research.backup.duration")
                .register(meterRegistry)
                .record(Duration.ofMillis(run.getDurationMs()));
        auditService.record(actorUserId, SecurityAuditEventType.BACKUP_COMPLETED);
        return manifest;
    }

    @Transactional
    public void failRun(
            UUID runId,
            String errorCode,
            String safeErrorMessage,
            UUID actorUserId
    ) {
        BackupRun run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Backup run not found."));
        run.setStatus(BackupRunStatus.FAILED);
        run.setErrorCode(errorCode);
        run.setErrorMessage(safeErrorMessage);
        finishTiming(run);
        meterRegistry.counter("research.backup.failure.count").increment();
        auditService.record(actorUserId, SecurityAuditEventType.BACKUP_FAILED);
    }

    private void saveArtifact(
            BackupRun run,
            BackupArtifactDescriptor descriptor
    ) {
        BackupArtifact artifact = new BackupArtifact();
        artifact.setBackupRun(run);
        artifact.setType(descriptor.type());
        artifact.setStorageReference(descriptor.storageReference());
        artifact.setSizeBytes(descriptor.sizeBytes());
        artifact.setSha256Checksum(descriptor.sha256Checksum());
        artifact.setStatus(BackupArtifactStatus.AVAILABLE);
        artifactRepository.save(artifact);
    }

    private void finishTiming(BackupRun run) {
        OffsetDateTime completedAt = OffsetDateTime.now();
        run.setCompletedAt(completedAt);
        run.setDurationMs(Duration.between(
                run.getStartedAt(),
                completedAt
        ).toMillis());
    }
}
