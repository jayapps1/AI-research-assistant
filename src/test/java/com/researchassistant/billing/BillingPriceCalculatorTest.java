package com.researchassistant.billing;

import com.researchassistant.billing.dto.BillingDtos.PlanPriceBreakdownResponse;
import com.researchassistant.subscription.BillingInterval;
import com.researchassistant.subscription.SubscriptionPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BillingPriceCalculatorTest {

    private BillingPriceCalculator calculator;
    private BillingPricingProperties properties;

    @BeforeEach
    void setUp() {
        properties = new BillingPricingProperties(
                new BigDecimal("0.02"), // 2% processing
                new BigDecimal("0.30")  // 30% AI generation
        );
        calculator = new BillingPriceCalculator(properties);
    }

    @Test
    void freePlanHasZeroFeesAndZeroTotal() {
        SubscriptionPlan freePlan = new SubscriptionPlan();
        freePlan.setCode("FREE");
        freePlan.setPrice(BigDecimal.ZERO);
        freePlan.setCurrency("GHS");

        PlanPriceBreakdownResponse breakdown = calculator.calculate(freePlan, BillingInterval.NONE);

        assertThat(breakdown.planCode()).isEqualTo("FREE");
        assertThat(breakdown.currency()).isEqualTo("GHS");
        assertThat(breakdown.baseAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(breakdown.processingAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(breakdown.aiGenerationAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(breakdown.totalAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void studentMonthlyPlanCalculates2PercentProcessingAnd30PercentAiGeneration() {
        SubscriptionPlan studentPlan = new SubscriptionPlan();
        studentPlan.setCode("STUDENT");
        studentPlan.setPrice(new BigDecimal("20.00"));
        studentPlan.setCurrency("GHS");

        PlanPriceBreakdownResponse breakdown = calculator.calculate(studentPlan, BillingInterval.MONTHLY);

        assertThat(breakdown.planCode()).isEqualTo("STUDENT");
        assertThat(breakdown.currency()).isEqualTo("GHS");
        // Base: 20.00
        assertThat(breakdown.baseAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
        // Processing (2%): 20.00 * 0.02 = 0.40
        assertThat(breakdown.processingAmount()).isEqualByComparingTo(new BigDecimal("0.40"));
        // AI Generation (30%): 20.00 * 0.30 = 6.00
        assertThat(breakdown.aiGenerationAmount()).isEqualByComparingTo(new BigDecimal("6.00"));
        // Total: 20.00 + 0.40 + 6.00 = 26.40
        assertThat(breakdown.totalAmount()).isEqualByComparingTo(new BigDecimal("26.40"));
    }

    @Test
    void proMonthlyPlanCalculates2PercentProcessingAnd30PercentAiGeneration() {
        SubscriptionPlan proPlan = new SubscriptionPlan();
        proPlan.setCode("PRO");
        proPlan.setPrice(new BigDecimal("100.00"));
        proPlan.setCurrency("GHS");

        PlanPriceBreakdownResponse breakdown = calculator.calculate(proPlan, BillingInterval.MONTHLY);

        assertThat(breakdown.planCode()).isEqualTo("PRO");
        assertThat(breakdown.currency()).isEqualTo("GHS");
        // Base: 100.00
        assertThat(breakdown.baseAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        // Processing (2%): 100.00 * 0.02 = 2.00
        assertThat(breakdown.processingAmount()).isEqualByComparingTo(new BigDecimal("2.00"));
        // AI Generation (30%): 100.00 * 0.30 = 30.00
        assertThat(breakdown.aiGenerationAmount()).isEqualByComparingTo(new BigDecimal("30.00"));
        // Total: 100.00 + 2.00 + 30.00 = 132.00
        assertThat(breakdown.totalAmount()).isEqualByComparingTo(new BigDecimal("132.00"));
    }

    @Test
    void calculateFromExplicitBaseAmount() {
        PlanPriceBreakdownResponse breakdown = calculator.calculate(
                "CUSTOM",
                BillingInterval.MONTHLY,
                new BigDecimal("50.00"),
                "GHS"
        );

        assertThat(breakdown.baseAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        // Processing (2%): 50.00 * 0.02 = 1.00
        assertThat(breakdown.processingAmount()).isEqualByComparingTo(new BigDecimal("1.00"));
        // AI Generation (30%): 50.00 * 0.30 = 15.00
        assertThat(breakdown.aiGenerationAmount()).isEqualByComparingTo(new BigDecimal("15.00"));
        // Total: 50.00 + 1.00 + 15.00 = 66.00
        assertThat(breakdown.totalAmount()).isEqualByComparingTo(new BigDecimal("66.00"));
    }
}
