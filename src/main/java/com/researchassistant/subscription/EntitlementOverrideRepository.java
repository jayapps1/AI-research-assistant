package com.researchassistant.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface EntitlementOverrideRepository extends JpaRepository<EntitlementOverride, UUID> {
    @Query("""
            select o from EntitlementOverride o
            where o.workspace.id = :workspaceId
              and o.feature = :feature
              and o.status = com.researchassistant.subscription.ComplimentaryAccessStatus.ACTIVE
              and o.startsAt <= :now
              and (o.expiresAt is null or o.expiresAt > :now)
            order by o.createdAt desc
            """)
    Optional<EntitlementOverride> findActive(@Param("workspaceId") UUID workspaceId, @Param("feature") PlanFeature feature, @Param("now") OffsetDateTime now);
}
