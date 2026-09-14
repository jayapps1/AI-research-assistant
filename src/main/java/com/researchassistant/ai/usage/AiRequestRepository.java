package com.researchassistant.ai.usage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiRequestRepository extends JpaRepository<AiRequest, UUID> {

    Optional<AiRequest> findByIdempotencyKey(String idempotencyKey);

    List<AiRequest> findAllByUserIdAndCreatedAtAfter(UUID userId, OffsetDateTime since);

    List<AiRequest> findAllByWorkspaceIdAndCreatedAtAfter(UUID workspaceId, OffsetDateTime since);

    List<AiRequest> findAllByProjectIdAndCreatedAtAfter(UUID projectId, OffsetDateTime since);

    long countByUserIdAndCreatedAtAfter(UUID userId, OffsetDateTime since);

    long countByWorkspaceIdAndCreatedAtAfter(UUID workspaceId, OffsetDateTime since);

    long countByUserIdAndStatusAndCreatedAtAfter(UUID userId, AiRequestStatus status, OffsetDateTime since);

    @Query("""
        SELECT COALESCE(SUM(r.totalTokens), 0)
        FROM AiRequest r
        WHERE r.user.id = :userId
          AND r.createdAt >= :since
          AND r.status = :status
        """)
    long sumTotalTokensByUserIdSinceAndStatus(@Param("userId") UUID userId, @Param("since") OffsetDateTime since, @Param("status") AiRequestStatus status);

    default long sumTotalTokensByUserIdSince(UUID userId, OffsetDateTime since) {
        return sumTotalTokensByUserIdSinceAndStatus(userId, since, AiRequestStatus.COMPLETED);
    }

    @Query("""
        SELECT COALESCE(SUM(r.totalTokens), 0)
        FROM AiRequest r
        WHERE r.workspace.id = :workspaceId
          AND r.createdAt >= :since
          AND r.status = :status
        """)
    long sumTotalTokensByWorkspaceIdSinceAndStatus(@Param("workspaceId") UUID workspaceId, @Param("since") OffsetDateTime since, @Param("status") AiRequestStatus status);

    default long sumTotalTokensByWorkspaceIdSince(UUID workspaceId, OffsetDateTime since) {
        return sumTotalTokensByWorkspaceIdSinceAndStatus(workspaceId, since, AiRequestStatus.COMPLETED);
    }

    @Query("""
        SELECT COALESCE(SUM(r.totalTokens), 0)
        FROM AiRequest r
        WHERE r.project.id = :projectId
          AND r.createdAt >= :since
          AND r.status = :status
        """)
    long sumTotalTokensByProjectIdSinceAndStatus(@Param("projectId") UUID projectId, @Param("since") OffsetDateTime since, @Param("status") AiRequestStatus status);

    default long sumTotalTokensByProjectIdSince(UUID projectId, OffsetDateTime since) {
        return sumTotalTokensByProjectIdSinceAndStatus(projectId, since, AiRequestStatus.COMPLETED);
    }
}
