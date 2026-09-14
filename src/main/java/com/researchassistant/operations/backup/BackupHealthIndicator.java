package com.researchassistant.operations.backup;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public class BackupHealthIndicator implements HealthIndicator {

    private static final Status DEGRADED = new Status("DEGRADED");

    private final BackupProperties properties;
    private final BackupPolicyRepository policyRepository;
    private final BackupRunRepository runRepository;

    public BackupHealthIndicator(
            BackupProperties properties,
            BackupPolicyRepository policyRepository,
            BackupRunRepository runRepository
    ) {
        this.properties = properties;
        this.policyRepository = policyRepository;
        this.runRepository = runRepository;
    }

    @Override
    public Health health() {
        if (!properties.enabled()) {
            return Health.up()
                    .withDetail("backup.enabled", false)
                    .withDetail("message", "Backup health is disabled by configuration.")
                    .build();
        }

        BackupPolicy policy = policyRepository
                .findFirstByStatusOrderByCreatedAtAsc(BackupPolicyStatus.ACTIVE)
                .orElse(null);
        if (policy == null) {
            return Health.status(DEGRADED)
                    .withDetail("reason", "No active backup policy.")
                    .build();
        }

        BackupRun lastSuccessful = runRepository
                .findFirstByStatusInOrderByCompletedAtDesc(List.of(
                        BackupRunStatus.COMPLETED,
                        BackupRunStatus.VERIFICATION_PENDING,
                        BackupRunStatus.VERIFIED
                ))
                .orElse(null);
        if (lastSuccessful == null || lastSuccessful.getCompletedAt() == null) {
            return Health.status(DEGRADED)
                    .withDetail("policy", policy.getName())
                    .withDetail("reason", "No successful backup run recorded.")
                    .build();
        }

        Duration expectedAge = expectedAge(policy)
                .plus(properties.verification().freshnessGrace());
        Duration actualAge = Duration.between(
                lastSuccessful.getCompletedAt(),
                OffsetDateTime.now()
        );
        Health.Builder builder = actualAge.compareTo(expectedAge) > 0
                ? Health.status(DEGRADED)
                : Health.up();

        runRepository
                .findFirstByStatusOrderByCompletedAtDesc(
                        BackupRunStatus.VERIFICATION_FAILED
                )
                .ifPresent(failed -> builder.withDetail(
                        "lastVerificationFailure",
                        failed.getCompletedAt()
                ));

        return builder
                .withDetail("policy", policy.getName())
                .withDetail("lastSuccessfulBackup", lastSuccessful.getCompletedAt())
                .withDetail("lastSuccessfulBackupAgeSeconds", actualAge.toSeconds())
                .withDetail("rpoMinutes", policy.getTargetRpoMinutes())
                .build();
    }

    private Duration expectedAge(BackupPolicy policy) {
        return switch (policy.getFrequency()) {
            case DAILY -> Duration.ofDays(1);
            case WEEKLY -> Duration.ofDays(7);
            case MONTHLY -> Duration.ofDays(31);
            case CUSTOM -> Duration.ofMinutes(
                    policy.getTargetRpoMinutes() == null
                            ? properties.target().rpoMinutes()
                            : policy.getTargetRpoMinutes()
            );
        };
    }
}
