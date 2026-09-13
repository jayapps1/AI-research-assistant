package com.researchassistant.security.totp;

import com.researchassistant.identity.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Encrypted authenticator-app TOTP credential for one user.
 *
 * <p>The encrypted secret is intentionally separated from the
 * primary user record. This keeps authentication credentials
 * independently manageable and leaves room for recovery codes or
 * additional credential types without expanding the users table.</p>
 */
@Entity
@Table(
        name = "totp_credentials",
        indexes = {
                @Index(
                        name = "idx_totp_credentials_enabled",
                        columnList = "enabled"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class TotpCredential {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_totp_credentials_user")
    )
    private User user;

    @Column(
            name = "encrypted_secret",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String encryptedSecret;

    @Column(
            name = "enabled",
            nullable = false
    )
    private boolean enabled;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private OffsetDateTime updatedAt;

    @Column(name = "last_used_timestep")
    private Long lastUsedTimestep;

    /**
     * Encrypted secret waiting for re-enrollment confirmation.
     *
     * <p>The current active secret remains usable until the pending
     * secret is successfully confirmed, preventing lockout if the
     * user fails to complete authenticator rotation.</p>
     */
    @Column(
            name = "pending_encrypted_secret",
            columnDefinition = "TEXT"
    )
    private String pendingEncryptedSecret;

    @Column(name = "pending_created_at")
    private OffsetDateTime pendingCreatedAt;

    @Column(name = "disabled_at")
    private OffsetDateTime disabledAt;

    @PrePersist
    protected void onCreate() {

        if (id == null) {
            id = UUID.randomUUID();
        }

        OffsetDateTime now = OffsetDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
