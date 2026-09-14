package com.researchassistant.operations.backup;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BackupManifestServiceTests {

    @Test
    void manifestContainsReferencesAndNoSecrets() throws Exception {
        BackupRun run = new BackupRun();
        run.setId(UUID.randomUUID());
        run.setApplicationVersion("research-assistant-api");
        run.setDatabaseSchemaVersion("14");
        run.setStatus(BackupRunStatus.VERIFICATION_PENDING);
        BackupArtifactDescriptor db = new BackupArtifactDescriptor(
                BackupArtifactType.POSTGRESQL_DUMP,
                "postgres://backup.dump",
                100L,
                "a".repeat(64),
                null
        );
        BackupArtifactDescriptor objects = new BackupArtifactDescriptor(
                BackupArtifactType.OBJECT_STORAGE_MANIFEST,
                "objects://manifest",
                50L,
                "b".repeat(64),
                3L
        );

        BackupManifest manifest =
                new BackupManifestService().createManifest(run, db, objects);
        String json = new ObjectMapper().writeValueAsString(manifest);

        assertThat(manifest.flywaySchemaVersion()).isEqualTo("14");
        assertThat(manifest.databaseBackupReference())
                .isEqualTo("postgres://backup.dump");
        assertThat(manifest.objectCount()).isEqualTo(3L);
        assertThat(json).doesNotContain(
                "JWT_SECRET",
                "CREDENTIAL_ENCRYPTION_KEY",
                "REDIS_PASSWORD",
                "POSTGRES_PASSWORD"
        );
    }

    @Test
    void manifestChecksumIsDeterministicForSameManifest() {
        ChecksumService checksumService = new ChecksumService();
        BackupManifest manifest = new BackupManifest(
                UUID.randomUUID(),
                java.time.OffsetDateTime.parse("2026-09-14T10:00:00Z"),
                "app",
                "14",
                "db",
                "a".repeat(64),
                "objects",
                "b".repeat(64),
                1L,
                "default",
                true,
                BackupRunStatus.VERIFIED
        );

        assertThat(checksumService.sha256(manifest))
                .isEqualTo(checksumService.sha256(manifest));
    }
}
