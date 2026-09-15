package com.researchassistant.billing;

import com.researchassistant.identity.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_idempotency_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_payment_idempotency_scope_key", columnNames = {"scope", "idempotency_key"}))
@Getter
@Setter
@NoArgsConstructor
public class PaymentIdempotencyRecord {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 80)
    private String scope;

    @Column(name = "idempotency_key", nullable = false, length = 180)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "payment_intent_id")
    private UUID paymentIntentId;

    @Column(name = "payment_attempt_id")
    private UUID paymentAttemptId;

    @Column(name = "response_json", columnDefinition = "TEXT")
    private String responseJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
