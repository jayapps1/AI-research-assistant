package com.researchassistant.billing;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.billing.pricing")
public record BillingPricingProperties(
        BigDecimal processingRate,
        BigDecimal aiGenerationRate
) {
    public BillingPricingProperties {
        if (processingRate == null) processingRate = new BigDecimal("0.02");
        if (aiGenerationRate == null) aiGenerationRate = new BigDecimal("0.30");
    }
}
