package com.researchassistant.operations.backup;

import com.researchassistant.operations.restore.RestoreRunRepository;
import com.researchassistant.operations.restore.RestoreRunStatus;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BackupRetentionServiceTests {

    @Test
    void activeRestoreBackupIsNotRetentionEligible() {
        BackupRun run = new BackupRun();
        run.setId(UUID.randomUUID());
        run.setStatus(BackupRunStatus.COMPLETED);
        run.setCompletedAt(OffsetDateTime.now().minusDays(30));
        BackupRunRepository runRepository = mock(BackupRunRepository.class);
        RestoreRunRepository restoreRunRepository =
                mock(RestoreRunRepository.class);
        when(runRepository.findAllByCompletedAtBeforeAndStatusIn(any(), any()))
                .thenReturn(List.of(run));
        when(restoreRunRepository.existsByBackupRunIdAndStatusIn(
                eq(run.getId()),
                any()
        )).thenReturn(true);
        BackupPolicy policy = policy();

        List<BackupRun> eligible = new BackupRetentionService(
                runRepository,
                restoreRunRepository
        ).eligibleForMetadataRetention(policy, OffsetDateTime.now());

        assertThat(eligible).isEmpty();
    }

    private BackupPolicy policy() {
        BackupPolicy policy = new BackupPolicy();
        policy.setRetentionDaily(7);
        policy.setRetentionWeekly(4);
        policy.setRetentionMonthly(12);
        return policy;
    }
}
