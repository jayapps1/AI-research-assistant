package com.researchassistant.identity.repository;

import com.researchassistant.identity.entity.PasswordResetAuthorization;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetAuthorizationRepository
        extends JpaRepository<PasswordResetAuthorization, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select authorization
            from PasswordResetAuthorization authorization
            join fetch authorization.user
            where authorization.tokenHash = :tokenHash
            """)
    Optional<PasswordResetAuthorization> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );
}
