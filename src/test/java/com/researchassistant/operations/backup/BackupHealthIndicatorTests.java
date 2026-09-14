package com.researchassistant.operations.backup;

import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BackupHealthIndicatorTests {

    @Test
    void disabledBackupHealthIsUpButExplicit() {
        BackupHealthIndicator indicator = new BackupHealthIndicator(
                properties(false),
                mock(BackupPolicyRepository.class),
                mock(BackupRunRepository.class)
        );

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void recentSuccessfulBackupIsHealthy() {
        BackupPolicyRepository policyRepository =
                mock(BackupPolicyRepository.class);
        BackupRunRepository runRepository = mock(BackupRunRepository.class);
        BackupPolicy policy = policy();
        BackupRun run = new BackupRun();
        run.setStatus(BackupRunStatus.VERIFIED);
        run.setCompletedAt(OffsetDateTime.now().minusHours(1));
        when(policyRepository.findFirstByStatusOrderByCreatedAtAsc(
                BackupPolicyStatus.ACTIVE
        )).thenReturn(Optional.of(policy));
        when(runRepository.findFirstByStatusInOrderByCompletedAtDesc(
                List.of(
                        BackupRunStatus.COMPLETED,
                        BackupRunStatus.VERIFICATION_PENDING,
                        BackupRunStatus.VERIFIED
                )
        )).thenReturn(Optional.of(run));
        when(runRepository.findFirstByStatusOrderByCompletedAtDesc(
                BackupRunStatus.VERIFICATION_FAILED
        )).thenReturn(Optional.empty());

        assertThat(new BackupHealthIndicator(
                properties(true),
                policyRepository,
                runRepository
        ).health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void overdueBackupIsDegraded() {
        BackupPolicyRepository policyRepository =
                mock(BackupPolicyRepository.class);
        BackupRunRepository runRepository = mock(BackupRunRepository.class);
        BackupPolicy policy = policy();
        BackupRun run = new BackupRun();
        run.setCompletedAt(OffsetDateTime.now().minusDays(3));
        when(policyRepository.findFirstByStatusOrderByCreatedAtAsc(
                BackupPolicyStatus.ACTIVE
        )).thenReturn(Optional.of(policy));
        when(runRepository.findFirstByStatusInOrderByCompletedAtDesc(
                List.of(
                        BackupRunStatus.COMPLETED,
                        BackupRunStatus.VERIFICATION_PENDING,
                        BackupRunStatus.VERIFIED
                )
        )).thenReturn(Optional.of(run));
        when(runRepository.findFirstByStatusOrderByCompletedAtDesc(
                BackupRunStatus.VERIFICATION_FAILED
        )).thenReturn(Optional.empty());

        assertThat(new BackupHealthIndicator(
                properties(true),
                policyRepository,
                runRepository
        ).health().getStatus().getCode()).isEqualTo("DEGRADED");
    }

    private BackupPolicy policy() {
        BackupPolicy policy = new BackupPolicy();
        policy.setName("default");
        policy.setFrequency(BackupFrequency.DAILY);
        policy.setTargetRpoMinutes(1440);
        return policy;
    }

    private BackupProperties properties(boolean enabled) {
        return new BackupProperties(
                enabled,
                "./data/backups",
                new BackupProperties.Retention(7, 4, 12),
                new BackupProperties.Verification(true, Duration.ofHours(6)),
                new BackupProperties.Target(1440, 240)
        );
    }
}
