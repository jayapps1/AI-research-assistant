package com.researchassistant.subscription;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "plan_entitlements",
        uniqueConstraints = @UniqueConstraint(name = "uk_plan_entitlements_plan_feature", columnNames = {"plan_id", "feature"}),
        indexes = @Index(name = "idx_plan_entitlements_plan", columnList = "plan_id"))
@Getter
@Setter
@NoArgsConstructor
public class PlanEntitlement {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private PlanFeature feature;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "limit_value")
    private Long limitValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_unit", length = 30)
    private LimitUnit limitUnit;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
