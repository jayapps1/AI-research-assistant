package com.researchassistant.operations.restore;

import com.researchassistant.operations.backup.BackupRun;
import com.researchassistant.operations.backup.BackupRunRepository;
import com.researchassistant.operations.backup.BackupRunStatus;
import com.researchassistant.operations.backup.FlywaySchemaVersionService;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestoreVerificationServiceTests {

    @Test
    void productionRestoreIsBlockedByServicePolicy() {
        RestoreVerificationService service =
                new RestoreVerificationService(
                        mock(BackupRunRepository.class),
                        mock(FlywaySchemaVersionService.class)
                );

        var outcome = service.verifyBackupCanBeRestored(
                UUID.randomUUID(),
                RestoreTargetEnvironment.PRODUCTION
        );

        assertThat(outcome.valid()).isFalse();
        assertThat(outcome.code()).contains("PRODUCTION");
    }

    @Test
    void onlyVerifiedMatchingSchemaBackupCanBeRestored() {
        UUID backupRunId = UUID.randomUUID();
        BackupRun run = new BackupRun();
        run.setId(backupRunId);
        run.setStatus(BackupRunStatus.VERIFIED);
        run.setDatabaseSchemaVersion("14");
        BackupRunRepository repository = mock(BackupRunRepository.class);
        FlywaySchemaVersionService schemaVersionService =
                mock(FlywaySchemaVersionService.class);
        when(repository.findById(backupRunId)).thenReturn(Optional.of(run));
        when(schemaVersionService.currentVersion()).thenReturn("14");
        RestoreVerificationService service =
                new RestoreVerificationService(repository, schemaVersionService);

        var outcome = service.verifyBackupCanBeRestored(
                backupRunId,
                RestoreTargetEnvironment.STAGING
        );

        assertThat(outcome.valid()).isTrue();
    }
}
