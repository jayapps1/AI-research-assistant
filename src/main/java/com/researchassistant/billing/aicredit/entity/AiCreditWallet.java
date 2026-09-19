package com.researchassistant.billing.aicredit.entity;

import com.researchassistant.workspace.entity.Workspace;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_credit_wallets",
        uniqueConstraints = @UniqueConstraint(name = "uk_ai_credit_wallets_workspace", columnNames = "workspace_id"),
        indexes = @Index(name = "idx_ai_credit_wallets_workspace", columnList = "workspace_id"))
@Getter
@Setter
@NoArgsConstructor
public class AiCreditWallet {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "purchased_balance", nullable = false, precision = 14, scale = 4)
    private BigDecimal purchasedBalance = BigDecimal.ZERO;

    @Column(name = "promotional_balance", nullable = false, precision = 14, scale = 4)
    private BigDecimal promotionalBalance = BigDecimal.ZERO;

    @Column(name = "reserved_balance", nullable = false, precision = 14, scale = 4)
    private BigDecimal reservedBalance = BigDecimal.ZERO;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public BigDecimal totalAvailableBalance() {
        BigDecimal purchased = purchasedBalance != null ? purchasedBalance : BigDecimal.ZERO;
        BigDecimal promotional = promotionalBalance != null ? promotionalBalance : BigDecimal.ZERO;
        return purchased.add(promotional);
    }

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (purchasedBalance == null) purchasedBalance = BigDecimal.ZERO;
        if (promotionalBalance == null) promotionalBalance = BigDecimal.ZERO;
        if (reservedBalance == null) reservedBalance = BigDecimal.ZERO;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
