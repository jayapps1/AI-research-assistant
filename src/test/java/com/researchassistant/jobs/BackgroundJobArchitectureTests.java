package com.researchassistant.jobs;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BackgroundJobArchitectureTests {
    @Test
    void jobStatusesSupportRetryAndDeadLetter() {
        assertThat(BackgroundJobStatus.values()).contains(BackgroundJobStatus.RETRY_SCHEDULED, BackgroundJobStatus.DEAD_LETTER);
    }

    @Test
    void repositoryClaimUsesPostgresSkipLocked() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/researchassistant/jobs/BackgroundJobRepository.java"));
        assertThat(source.toLowerCase()).contains("for update skip locked");
    }

    @Test
    void outboxMigrationExists() throws Exception {
        String migration = Files.readString(Path.of("src/main/resources/db/migration/V21__payment_recovery_complimentary_access_jobs_file_security.sql"));
        assertThat(migration).contains("outbox_events", "payload_version");
    }
}
