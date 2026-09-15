package com.researchassistant.billing;

import com.researchassistant.identity.entity.User;
import com.researchassistant.subscription.BillingInterval;
import com.researchassistant.workspace.entity.Workspace;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_transactions_internal_reference", columnNames = "internal_reference"),
                @UniqueConstraint(name = "uk_payment_transactions_provider_reference", columnNames = {"provider", "environment", "provider_reference"})
        },
        indexes = {
                @Index(name = "idx_payment_transactions_workspace_created", columnList = "workspace_id,created_at"),
                @Index(name = "idx_payment_transactions_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
public class PaymentTransaction {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "initiated_by", nullable = false)
    private User initiatedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PaymentProviderType provider = PaymentProviderType.PAYSTACK;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentEnvironment environment = PaymentEnvironment.TEST;

    @Column(name = "provider_reference", length = 255)
    private String providerReference;

    @Column(name = "internal_reference", nullable = false, length = 80)
    private String internalReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PaymentTransactionType type = PaymentTransactionType.SUBSCRIPTION_PURCHASE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PaymentTransactionStatus status = PaymentTransactionStatus.INITIALIZED;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "plan_id")
    private UUID planId;

    @Column(name = "plan_code", length = 60)
    private String planCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", length = 20)
    private BillingInterval billingInterval;

    @Column(name = "authorization_url", length = 1000)
    private String authorizationUrl;

    @Column(name = "access_code", length = 255)
    private String accessCode;

    @Column(name = "initiated_at", nullable = false)
    private OffsetDateTime initiatedAt;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        if (initiatedAt == null) initiatedAt = now;
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
