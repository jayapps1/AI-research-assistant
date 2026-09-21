package com.researchassistant.ai.usage;

import com.researchassistant.ai.provider.AiProviderType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_model_pricing")
public class AiModelPricing {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 40)
    private AiProviderType provider;

    @Column(name = "model", nullable = false, length = 200)
    private String model;

    @Column(name = "input_price_per_million", precision = 12, scale = 6)
    private BigDecimal inputPricePerMillion;

    @Column(name = "cached_input_price_per_million", precision = 12, scale = 6)
    private BigDecimal cachedInputPricePerMillion;

    @Column(name = "output_price_per_million", precision = 12, scale = 6)
    private BigDecimal outputPricePerMillion;

    @Column(name = "service_tier", length = 50)
    private String serviceTier;

    @Column(name = "context_class", length = 50)
    private String contextClass;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "USD";

    @Column(name = "effective_from", nullable = false)
    private OffsetDateTime effectiveFrom;

    @Column(name = "effective_to")
    private OffsetDateTime effectiveTo;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public AiProviderType getProvider() { return provider; }
    public void setProvider(AiProviderType provider) { this.provider = provider; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public BigDecimal getInputPricePerMillion() { return inputPricePerMillion; }
    public void setInputPricePerMillion(BigDecimal inputPricePerMillion) { this.inputPricePerMillion = inputPricePerMillion; }

    public BigDecimal getCachedInputPricePerMillion() { return cachedInputPricePerMillion; }
    public void setCachedInputPricePerMillion(BigDecimal cachedInputPricePerMillion) { this.cachedInputPricePerMillion = cachedInputPricePerMillion; }

    public BigDecimal getOutputPricePerMillion() { return outputPricePerMillion; }
    public void setOutputPricePerMillion(BigDecimal outputPricePerMillion) { this.outputPricePerMillion = outputPricePerMillion; }

    public String getServiceTier() { return serviceTier; }
    public void setServiceTier(String serviceTier) { this.serviceTier = serviceTier; }

    public String getContextClass() { return contextClass; }
    public void setContextClass(String contextClass) { this.contextClass = contextClass; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public OffsetDateTime getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(OffsetDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public OffsetDateTime getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(OffsetDateTime effectiveTo) { this.effectiveTo = effectiveTo; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
