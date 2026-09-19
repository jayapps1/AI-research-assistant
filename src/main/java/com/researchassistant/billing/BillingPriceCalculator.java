package com.researchassistant.billing;

import com.researchassistant.billing.dto.BillingDtos.PlanPriceBreakdownResponse;
import com.researchassistant.subscription.BillingInterval;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@EnableConfigurationProperties(BillingPricingProperties.class)
public class BillingPriceCalculator {

    private final BillingPricingProperties properties;

    public BillingPriceCalculator(BillingPricingProperties properties) {
        this.properties = properties;
    }

    public PlanPriceBreakdownResponse calculate(com.researchassistant.subscription.SubscriptionPlan plan, BillingInterval interval) {
        if (plan == null) {
            return calculate("UNKNOWN", interval, BigDecimal.ZERO, "GHS");
        }
        return calculate(plan.getCode(), interval, plan.getPrice(), plan.getCurrency());
    }

    public PlanPriceBreakdownResponse calculate(String planCode, BillingInterval interval, BigDecimal basePrice, String currency) {
        String safeCurrency = currency == null || currency.isBlank() ? "GHS" : currency;
        if (basePrice == null || basePrice.signum() <= 0 || "FREE".equalsIgnoreCase(planCode) || interval == BillingInterval.NONE) {
            return new PlanPriceBreakdownResponse(
                    planCode,
                    interval,
                    safeCurrency,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    properties.processingRate(),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    properties.aiGenerationRate(),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            );
        }

        BigDecimal base = basePrice.setScale(2, RoundingMode.HALF_UP);
        BigDecimal procRate = properties.processingRate();
        BigDecimal aiRate = properties.aiGenerationRate();

        BigDecimal procAmount = base.multiply(procRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal aiAmount = base.multiply(aiRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = base.add(procAmount).add(aiAmount).setScale(2, RoundingMode.HALF_UP);

        return new PlanPriceBreakdownResponse(
                planCode,
                interval,
                safeCurrency,
                base,
                procRate,
                procAmount,
                aiRate,
                aiAmount,
                total
        );
    }
}
