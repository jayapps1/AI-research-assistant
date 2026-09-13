package com.researchassistant.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Short-lived authorization to complete a password reset.
 *
 * <p>This is deliberately not a normal access token. It grants no
 * general API access and stores only a hash of the opaque token
 * returned to the client.</p>
 */
@Entity
@Table(
        name = "password_reset_authorizations",
        indexes = {
                @Index(
                        name = "idx_password_reset_authorizations_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_password_reset_authorizations_expires_at",
                        columnList = "expires_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetAuthorization {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_password_reset_authorizations_user"
            )
    )
    private User user;

    @Column(
            name = "token_hash",
            nullable = false,
            unique = true,
            length = 64
    )
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "verification_method",
            nullable = false,
            length = 30
    )
    private PasswordRecoveryMethod verificationMethod;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @PrePersist
    protected void onCreate() {

        if (id == null) {
            id = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
