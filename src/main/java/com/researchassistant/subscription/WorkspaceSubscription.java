package com.researchassistant.subscription;

import com.researchassistant.workspace.entity.Workspace;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "workspace_subscriptions",
        indexes = {
                @Index(name = "idx_workspace_subscriptions_workspace_status", columnList = "workspace_id,status"),
                @Index(name = "idx_workspace_subscriptions_period", columnList = "current_period_start,current_period_end")
        })
@Getter
@Setter
@NoArgsConstructor
public class WorkspaceSubscription {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkspaceSubscriptionStatus status = WorkspaceSubscriptionStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", nullable = false, length = 20)
    private BillingInterval billingInterval = BillingInterval.NONE;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "current_period_start", nullable = false)
    private OffsetDateTime currentPeriodStart;

    @Column(name = "current_period_end", nullable = false)
    private OffsetDateTime currentPeriodEnd;

    @Column(name = "cancel_at")
    private OffsetDateTime cancelAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "auto_renew", nullable = false)
    private boolean autoRenew;

    @Column(name = "external_subscription_reference", length = 255)
    private String externalSubscriptionReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_source", nullable = false, length = 30)
    private SubscriptionAccessSource accessSource = SubscriptionAccessSource.FREE_DEFAULT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        if (startsAt == null) startsAt = now;
        if (currentPeriodStart == null) currentPeriodStart = startsAt;
        if (currentPeriodEnd == null) currentPeriodEnd = currentPeriodStart.plusMonths(1);
        if (accessSource == null) accessSource = SubscriptionAccessSource.FREE_DEFAULT;
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
