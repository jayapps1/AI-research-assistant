package com.researchassistant.billing.aicredit.entity;

import com.researchassistant.identity.entity.User;
import com.researchassistant.workspace.entity.Workspace;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_credit_ledger_entries",
        uniqueConstraints = @UniqueConstraint(name = "uk_ai_credit_ledger_idempotency", columnNames = "idempotency_key"),
        indexes = {
                @Index(name = "idx_ai_credit_ledger_workspace_created", columnList = "workspace_id,created_at"),
                @Index(name = "idx_ai_credit_ledger_type", columnList = "type"),
                @Index(name = "idx_ai_credit_ledger_ai_request", columnList = "ai_request_id"),
                @Index(name = "idx_ai_credit_ledger_purchase", columnList = "purchase_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class AiCreditLedgerEntry {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiCreditBucket bucket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AiCreditLedgerType type;

    @Column(name = "credit_amount", nullable = false, precision = 14, scale = 4)
    private BigDecimal creditAmount;

    @Column(name = "balance_before", nullable = false, precision = 14, scale = 4)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 14, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "source_type", nullable = false, length = 60)
    private String sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "ai_request_id")
    private UUID aiRequestId;

    @Column(name = "payment_intent_id")
    private UUID paymentIntentId;

    @Column(name = "payment_attempt_id")
    private UUID paymentAttemptId;

    @Column(name = "purchase_id")
    private UUID purchaseId;

    @Column(name = "idempotency_key", length = 180)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
