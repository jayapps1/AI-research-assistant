package com.researchassistant.billing.aicredit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_credit_rates",
        indexes = @Index(name = "idx_ai_credit_rates_provider_model_active", columnList = "provider,model,active"))
@Getter
@Setter
@NoArgsConstructor
public class AiCreditRate {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(nullable = false, length = 200)
    private String model;

    @Column(name = "credits_per_million_input_tokens", nullable = false, precision = 14, scale = 4)
    private BigDecimal creditsPerMillionInputTokens;

    @Column(name = "credits_per_million_output_tokens", nullable = false, precision = 14, scale = 4)
    private BigDecimal creditsPerMillionOutputTokens;

    @Column(name = "credits_per_million_cached_input_tokens", precision = 14, scale = 4)
    private BigDecimal creditsPerMillionCachedInputTokens;

    @Column(name = "effective_from", nullable = false)
    private OffsetDateTime effectiveFrom;

    @Column(name = "effective_to")
    private OffsetDateTime effectiveTo;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
