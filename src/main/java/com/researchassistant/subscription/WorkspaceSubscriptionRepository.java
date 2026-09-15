package com.researchassistant.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceSubscriptionRepository extends JpaRepository<WorkspaceSubscription, UUID> {
    @Query("""
            select s from WorkspaceSubscription s
            where s.workspace.id = :workspaceId
              and s.status in (com.researchassistant.subscription.WorkspaceSubscriptionStatus.TRIALING,
                               com.researchassistant.subscription.WorkspaceSubscriptionStatus.ACTIVE,
                               com.researchassistant.subscription.WorkspaceSubscriptionStatus.PAST_DUE,
                               com.researchassistant.subscription.WorkspaceSubscriptionStatus.SUSPENDED)
              and s.currentPeriodStart <= :now
              and s.currentPeriodEnd > :now
            order by s.createdAt desc
            """)
    List<WorkspaceSubscription> findEffectiveCandidates(@Param("workspaceId") UUID workspaceId, @Param("now") OffsetDateTime now);

    default Optional<WorkspaceSubscription> findCurrentEffective(UUID workspaceId, OffsetDateTime now) {
        return findEffectiveCandidates(workspaceId, now).stream().findFirst();
    }
}
