package com.researchassistant.billing.aicredit.entity;

import com.researchassistant.billing.BillingPaymentIntent;
import com.researchassistant.workspace.entity.Workspace;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_credit_purchases",
        indexes = {
                @Index(name = "idx_ai_credit_purchases_workspace_created", columnList = "workspace_id,created_at"),
                @Index(name = "idx_ai_credit_purchases_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
public class AiCreditPurchase {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_pack_id", nullable = false)
    private AiCreditPack creditPack;

    @Column(name = "pack_code", nullable = false, length = 60)
    private String packCode;

    @Column(name = "pack_name", nullable = false, length = 160)
    private String packName;

    @Column(name = "credits_purchased", nullable = false, precision = 14, scale = 4)
    private BigDecimal creditsPurchased;

    @Column(name = "price_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAmount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AiCreditPurchaseStatus status = AiCreditPurchaseStatus.CREATED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_payment_intent_id")
    private BillingPaymentIntent billingPaymentIntent;

    @Column(name = "credited_at")
    private OffsetDateTime creditedAt;

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
