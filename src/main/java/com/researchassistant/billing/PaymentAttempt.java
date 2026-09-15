package com.researchassistant.billing;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_attempts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_attempts_internal_reference", columnNames = "internal_reference"),
                @UniqueConstraint(name = "uk_payment_attempts_provider_reference", columnNames = {"provider", "environment", "provider_reference"}),
                @UniqueConstraint(name = "uk_payment_attempts_intent_attempt", columnNames = {"payment_intent_id", "attempt_number"})
        },
        indexes = {
                @Index(name = "idx_payment_attempts_intent", columnList = "payment_intent_id,attempt_number"),
                @Index(name = "idx_payment_attempts_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
public class PaymentAttempt {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_intent_id", nullable = false)
    private BillingPaymentIntent paymentIntent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PaymentProviderType provider = PaymentProviderType.PAYSTACK;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentEnvironment environment = PaymentEnvironment.TEST;

    @Column(name = "internal_reference", nullable = false, length = 80)
    private String internalReference;

    @Column(name = "provider_reference", length = 255)
    private String providerReference;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PaymentAttemptStatus status = PaymentAttemptStatus.CREATED;

    @Column(name = "expected_amount", nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal expectedAmount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "provider_status", length = 80)
    private String providerStatus;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message_safe", length = 500)
    private String failureMessageSafe;

    @Column(name = "initialized_at")
    private OffsetDateTime initializedAt;

    @Column(name = "provider_verified_at")
    private OffsetDateTime providerVerifiedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "authorization_url", length = 1000)
    private String authorizationUrl;

    @Column(name = "access_code", length = 255)
    private String accessCode;

    @Column(name = "requires_review", nullable = false)
    private boolean requiresReview;

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
