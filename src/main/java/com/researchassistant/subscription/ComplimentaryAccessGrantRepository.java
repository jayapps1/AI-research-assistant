package com.researchassistant.subscription;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ComplimentaryAccessGrantRepository extends JpaRepository<ComplimentaryAccessGrant, UUID> {
    @Query("""
            select g from ComplimentaryAccessGrant g
            where g.workspace.id = :workspaceId
              and g.status = com.researchassistant.subscription.ComplimentaryAccessStatus.ACTIVE
              and g.startsAt <= :now
              and (g.expiresAt is null or g.expiresAt > :now)
              and g.plan is not null
            order by g.createdAt desc
            """)
    List<ComplimentaryAccessGrant> findActiveWorkspacePlanGrants(@Param("workspaceId") UUID workspaceId, @Param("now") OffsetDateTime now);

    Page<ComplimentaryAccessGrant> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
