package com.researchassistant.jobs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BackgroundJobRepository extends JpaRepository<BackgroundJob, UUID> {
    @Query(value = """
            select *
            from background_jobs
            where status in ('QUEUED','RETRY_SCHEDULED')
              and (next_attempt_at is null or next_attempt_at <= current_timestamp)
            order by created_at
            for update skip locked
            limit 1
            """, nativeQuery = true)
    Optional<BackgroundJob> claimNextForUpdateSkipLocked();

    long countByStatus(BackgroundJobStatus status);
}
