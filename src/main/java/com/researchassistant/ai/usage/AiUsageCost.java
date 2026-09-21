package com.researchassistant.ai.usage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_usage_costs")
public class AiUsageCost {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private AiRequest request;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "USD";

    @Column(name = "input_cost", precision = 12, scale = 6)
    private BigDecimal inputCost;

    @Column(name = "cached_input_cost", precision = 12, scale = 6)
    private BigDecimal cachedInputCost;

    @Column(name = "output_cost", precision = 12, scale = 6)
    private BigDecimal outputCost;

    @Column(name = "total_cost", precision = 12, scale = 6)
    private BigDecimal totalCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 40)
    private AiCostSource source;

    @Column(name = "pricing_version", length = 100)
    private String pricingVersion;

    @Column(name = "calculated_at", nullable = false, updatable = false)
    private OffsetDateTime calculatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (calculatedAt == null) {
            calculatedAt = OffsetDateTime.now();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public AiRequest getRequest() { return request; }
    public void setRequest(AiRequest request) { this.request = request; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getInputCost() { return inputCost; }
    public void setInputCost(BigDecimal inputCost) { this.inputCost = inputCost; }

    public BigDecimal getCachedInputCost() { return cachedInputCost; }
    public void setCachedInputCost(BigDecimal cachedInputCost) { this.cachedInputCost = cachedInputCost; }

    public BigDecimal getOutputCost() { return outputCost; }
    public void setOutputCost(BigDecimal outputCost) { this.outputCost = outputCost; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public AiCostSource getSource() { return source; }
    public void setSource(AiCostSource source) { this.source = source; }

    public String getPricingVersion() { return pricingVersion; }
    public void setPricingVersion(String pricingVersion) { this.pricingVersion = pricingVersion; }

    public OffsetDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(OffsetDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
}
