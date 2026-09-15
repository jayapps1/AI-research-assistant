package com.researchassistant.subscription;

import com.researchassistant.identity.entity.User;
import com.researchassistant.workspace.entity.Workspace;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "entitlement_overrides",
        indexes = @Index(name = "idx_entitlement_overrides_workspace_feature", columnList = "workspace_id,feature,status"))
@Getter
@Setter
@NoArgsConstructor
public class EntitlementOverride {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private PlanFeature feature;

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_mode", nullable = false, length = 20)
    private EntitlementLimitMode limitMode = EntitlementLimitMode.LIMITED;

    @Column(name = "limit_value")
    private Long limitValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_unit", length = 30)
    private LimitUnit limitUnit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ComplimentaryAccessStatus status = ComplimentaryAccessStatus.ACTIVE;

    @Column(nullable = false, length = 1000)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "granted_by", nullable = false)
    private User grantedBy;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        if (startsAt == null) startsAt = now;
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
