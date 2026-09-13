package com.researchassistant.security.jwt;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

/**
 * Persistence access for refresh sessions.
 */
public interface RefreshSessionRepository
        extends JpaRepository<RefreshSession, UUID> {

    /**
     * Locks the matching refresh session for the current
     * transaction so competing refresh requests cannot rotate the
     * same token at the same time.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select session
            from RefreshSession session
            join fetch session.user
            where session.tokenHash = :tokenHash
            """)
    Optional<RefreshSession> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    /**
     * Revokes active refresh sessions after password reset or
     * similar account-security events.
     */
    @Modifying
    @Query("""
            update RefreshSession session
            set session.revokedAt = CURRENT_TIMESTAMP
            where session.user.id = :userId
              and session.revokedAt is null
            """)
    int revokeActiveSessionsForUser(@Param("userId") UUID userId);

    List<RefreshSession> findByUserIdAndRevokedAtIsNull(UUID userId);
}
