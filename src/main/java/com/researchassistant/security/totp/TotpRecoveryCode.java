package com.researchassistant.security.totp;

import com.researchassistant.identity.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * One-time recovery code for users with TOTP enabled.
 *
 * <p>The plaintext code is shown once during generation. Only a
 * slow password-style hash is stored, so the server cannot recover
 * or display existing codes later.</p>
 */
@Entity
@Table(
        name = "totp_recovery_codes",
        indexes = {
                @Index(
                        name = "idx_totp_recovery_codes_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_totp_recovery_codes_available",
                        columnList = "user_id, used_at, revoked_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class TotpRecoveryCode {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_totp_recovery_codes_user")
    )
    private User user;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

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
