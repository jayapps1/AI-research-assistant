package com.researchassistant.subscription;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscription_plans",
        uniqueConstraints = @UniqueConstraint(name = "uk_subscription_plans_code", columnNames = "code"),
        indexes = {
                @Index(name = "idx_subscription_plans_status", columnList = "status"),
                @Index(name = "idx_subscription_plans_public_order", columnList = "publicly_available,display_order"),
                @Index(name = "idx_subscription_plans_featured", columnList = "featured")
        })
@Getter
@Setter
@NoArgsConstructor
public class SubscriptionPlan {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 60)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubscriptionPlanStatus status = SubscriptionPlanStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", nullable = false, length = 20)
    private BillingInterval billingInterval = BillingInterval.NONE;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(nullable = false, length = 10)
    private String currency = "GHS";

    @Column(name = "publicly_available", nullable = false)
    private boolean publiclyAvailable;

    @Column(name = "featured", nullable = false)
    private boolean featured;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

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
        code = code == null ? null : code.trim().toUpperCase();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
        code = code == null ? null : code.trim().toUpperCase();
    }
}
