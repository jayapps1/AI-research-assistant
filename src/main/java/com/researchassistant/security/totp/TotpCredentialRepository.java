package com.researchassistant.security.totp;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for authenticator-app TOTP credentials.
 */
public interface TotpCredentialRepository
        extends JpaRepository<TotpCredential, UUID> {

    Optional<TotpCredential> findByUserId(UUID userId);

    /**
     * Locks the credential row for verification so replay
     * protection can atomically compare and advance the last-used
     * timestep.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select credential
            from TotpCredential credential
            join fetch credential.user
            where credential.user.id = :userId
            """)
    Optional<TotpCredential> findByUserIdForUpdate(
            @Param("userId") UUID userId
    );
}
