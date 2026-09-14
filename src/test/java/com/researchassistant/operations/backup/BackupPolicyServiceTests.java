package com.researchassistant.operations.backup;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class BackupPolicyServiceTests {

    @Test
    void defaultPolicyUsesConfiguredRetentionAndTargets() {
        BackupPolicyService service = new BackupPolicyService(
                mock(BackupPolicyRepository.class),
                properties()
        );
        BackupPolicy policy = new BackupPolicy();
        policy.setName("default");
        policy.setStatus(BackupPolicyStatus.ACTIVE);
        policy.setFrequency(BackupFrequency.DAILY);
        policy.setRetentionDaily(7);
        policy.setRetentionWeekly(4);
        policy.setRetentionMonthly(12);
        policy.setTargetRpoMinutes(1440);
        policy.setTargetRtoMinutes(240);

        service.validate(policy);

        assertThat(policy.getRetentionDaily()).isEqualTo(7);
        assertThat(policy.getTargetRpoMinutes()).isEqualTo(1440);
    }

    @Test
    void negativeRetentionIsRejected() {
        BackupPolicyService service = new BackupPolicyService(
                mock(BackupPolicyRepository.class),
                properties()
        );
        BackupPolicy policy = new BackupPolicy();
        policy.setRetentionDaily(-1);
        policy.setRetentionWeekly(4);
        policy.setRetentionMonthly(12);

        assertThatThrownBy(() -> service.validate(policy))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("retention");
    }

    @Test
    void invalidRpoAndRtoAreRejected() {
        BackupPolicyService service = new BackupPolicyService(
                mock(BackupPolicyRepository.class),
                properties()
        );
        BackupPolicy policy = new BackupPolicy();
        policy.setRetentionDaily(7);
        policy.setRetentionWeekly(4);
        policy.setRetentionMonthly(12);
        policy.setTargetRpoMinutes(0);

        assertThatThrownBy(() -> service.validate(policy))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RPO");
    }

    private BackupProperties properties() {
        return new BackupProperties(
                false,
                "./data/backups",
                new BackupProperties.Retention(7, 4, 12),
                new BackupProperties.Verification(true, Duration.ofHours(6)),
                new BackupProperties.Target(1440, 240)
        );
    }
}
