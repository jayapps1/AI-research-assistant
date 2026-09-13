package com.researchassistant.security.jwt;

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
 * Server-side record for an opaque refresh token.
 *
 * <p>Only a SHA-256 token hash is stored. The raw token is a
 * bearer credential and is returned to the client once, then
 * discarded by the backend.</p>
 */
@Entity
@Table(
        name = "refresh_sessions",
        indexes = {
                @Index(
                        name = "idx_refresh_sessions_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_refresh_sessions_expires_at",
                        columnList = "expires_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class RefreshSession {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    /**
     * Owning user. No cascade is configured because refresh
     * sessions are security records, not child profile data.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_refresh_sessions_user")
    )
    private User user;

    @Column(
            name = "token_hash",
            nullable = false,
            unique = true,
            length = 64
    )
    private String tokenHash;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private OffsetDateTime expiresAt;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "replaced_by_session_id")
    private UUID replacedBySessionId;

    @Column(
            name = "user_agent",
            length = 512
    )
    private String userAgent;

    @Column(
            name = "ip_address",
            length = 64
    )
    private String ipAddress;

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
