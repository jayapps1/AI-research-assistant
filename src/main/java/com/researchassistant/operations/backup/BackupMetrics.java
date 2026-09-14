package com.researchassistant.operations.backup;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public class BackupMetrics {

    private final BackupRunRepository runRepository;

    public BackupMetrics(
            BackupRunRepository runRepository,
            MeterRegistry meterRegistry
    ) {
        this.runRepository = runRepository;
        Gauge.builder(
                        "research.backup.age.seconds",
                        this,
                        BackupMetrics::lastSuccessfulBackupAgeSeconds
                )
                .description("Age in seconds of the latest successful backup.")
                .register(meterRegistry);
        Gauge.builder(
                        "research.backup.verified.age.seconds",
                        this,
                        BackupMetrics::lastVerifiedBackupAgeSeconds
                )
                .description("Age in seconds of the latest verified backup.")
                .register(meterRegistry);
    }

    double lastSuccessfulBackupAgeSeconds() {
        return runRepository.findFirstByStatusInOrderByCompletedAtDesc(List.of(
                        BackupRunStatus.COMPLETED,
                        BackupRunStatus.VERIFICATION_PENDING,
                        BackupRunStatus.VERIFIED
                ))
                .map(BackupRun::getCompletedAt)
                .map(this::ageSeconds)
                .orElse(Double.NaN);
    }

    double lastVerifiedBackupAgeSeconds() {
        return runRepository
                .findFirstByStatusOrderByCompletedAtDesc(BackupRunStatus.VERIFIED)
                .map(BackupRun::getCompletedAt)
                .map(this::ageSeconds)
                .orElse(Double.NaN);
    }

    private double ageSeconds(OffsetDateTime time) {
        if (time == null) {
            return Double.NaN;
        }
        return Duration.between(time, OffsetDateTime.now()).toSeconds();
    }
}
