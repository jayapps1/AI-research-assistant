package com.researchassistant.ai.usage;

import com.researchassistant.ai.provider.AiProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiProviderCostCalculatorTest {

    @Mock
    private AiModelPricingRepository pricingRepository;

    private AiProviderCostCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new AiProviderCostCalculator(pricingRepository);
    }

    @Test
    @DisplayName("Calculates gpt-5.6-luna cost and platform credits without cache")
    void testGpt5LunaUncached() {
        when(pricingRepository.findFirstByProviderAndModelAndActiveIsTrueAndEffectiveFromBeforeOrderByEffectiveFromDesc(
                eq(AiProviderType.OPENAI), eq("gpt-5.6-luna"), any()
        )).thenReturn(Optional.empty()); // uses defaults: input 0.20/M, cached 0.02/M, output 1.20/M

        // 10,000 input @ 0.20/M = 0.002000
        // 2,000 output @ 1.20/M = 0.002400
        // total = 0.004400 USD -> 4.4000 credits
        AiProviderCostCalculator.CostBreakdown breakdown = calculator.calculateGenerationCost(
                "OPENAI", "gpt-5.6-luna", 10_000, 2_000, 0
        );

        assertThat(breakdown.inputCost()).isEqualByComparingTo("0.002000");
        assertThat(breakdown.cachedInputCost()).isEqualByComparingTo("0.000000");
        assertThat(breakdown.outputCost()).isEqualByComparingTo("0.002400");
        assertThat(breakdown.totalCost()).isEqualByComparingTo("0.004400");
        assertThat(breakdown.credits()).isEqualByComparingTo("4.4000");
    }

    @Test
    @DisplayName("Calculates gpt-5.6-luna cost and credits with prompt caching")
    void testGpt5LunaWithCachedInput() {
        when(pricingRepository.findFirstByProviderAndModelAndActiveIsTrueAndEffectiveFromBeforeOrderByEffectiveFromDesc(
                eq(AiProviderType.OPENAI), eq("gpt-5.6-luna"), any()
        )).thenReturn(Optional.empty());

        // 10,000 prompt tokens total: 8,000 cached @ 0.02/M = 0.000160; 2,000 uncached @ 0.20/M = 0.000400
        // 1,000 output @ 1.20/M = 0.001200
        // total = 0.001760 USD -> 1.7600 credits
        AiProviderCostCalculator.CostBreakdown breakdown = calculator.calculateGenerationCost(
                "OPENAI", "gpt-5.6-luna", 10_000, 1_000, 8_000
        );

        assertThat(breakdown.inputCost()).isEqualByComparingTo("0.000400");
        assertThat(breakdown.cachedInputCost()).isEqualByComparingTo("0.000160");
        assertThat(breakdown.outputCost()).isEqualByComparingTo("0.001200");
        assertThat(breakdown.totalCost()).isEqualByComparingTo("0.001760");
        assertThat(breakdown.credits()).isEqualByComparingTo("1.7600");
    }

    @Test
    @DisplayName("Calculates text-embedding-3-small cost and credits")
    void testTextEmbeddingCost() {
        when(pricingRepository.findFirstByProviderAndModelAndActiveIsTrueAndEffectiveFromBeforeOrderByEffectiveFromDesc(
                eq(AiProviderType.OPENAI), eq("text-embedding-3-small"), any()
        )).thenReturn(Optional.empty()); // default 0.02/M

        // 50,000 tokens @ 0.02/M = 0.001000 USD -> 1.0000 credit
        AiProviderCostCalculator.CostBreakdown breakdown = calculator.calculateEmbeddingCost(
                "OPENAI", "text-embedding-3-small", 50_000
        );

        assertThat(breakdown.totalCost()).isEqualByComparingTo("0.001000");
        assertThat(breakdown.credits()).isEqualByComparingTo("1.0000");
    }

    @Test
    @DisplayName("Minimum non-zero charge is 0.0001 credit")
    void testMinimumCreditCharge() {
        when(pricingRepository.findFirstByProviderAndModelAndActiveIsTrueAndEffectiveFromBeforeOrderByEffectiveFromDesc(
                eq(AiProviderType.OPENAI), eq("gpt-5.6-luna"), any()
        )).thenReturn(Optional.empty());

        // 1 token input @ 0.20/M = 0.0000002 -> credits round to 0.0002 -> non-zero
        AiProviderCostCalculator.CostBreakdown breakdown = calculator.calculateGenerationCost(
                "OPENAI", "gpt-5.6-luna", 1, 0, 0
        );

        assertThat(breakdown.credits()).isGreaterThan(BigDecimal.ZERO);
    }
}
