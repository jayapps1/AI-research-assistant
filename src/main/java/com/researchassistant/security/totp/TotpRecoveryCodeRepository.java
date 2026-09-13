package com.researchassistant.security.totp;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TotpRecoveryCodeRepository
        extends JpaRepository<TotpRecoveryCode, UUID> {

    List<TotpRecoveryCode> findByUserIdAndUsedAtIsNullAndRevokedAtIsNull(
            UUID userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select code
            from TotpRecoveryCode code
            join fetch code.user
            where code.user.id = :userId
              and code.usedAt is null
              and code.revokedAt is null
            """)
    List<TotpRecoveryCode> findAvailableForUserForUpdate(
            @Param("userId") UUID userId
    );
}
