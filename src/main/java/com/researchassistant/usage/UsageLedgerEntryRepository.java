package com.researchassistant.usage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface UsageLedgerEntryRepository extends JpaRepository<UsageLedgerEntry, UUID> {
    boolean existsByIdempotencyKey(String idempotencyKey);

    @Query("""
            select coalesce(sum(e.quantity), 0)
            from UsageLedgerEntry e
            where e.workspace.id = :workspaceId
              and e.metric = :metric
              and e.occurredAt >= :start
              and e.occurredAt < :end
            """)
    long sumQuantity(@Param("workspaceId") UUID workspaceId, @Param("metric") UsageMetricType metric,
                     @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
}
