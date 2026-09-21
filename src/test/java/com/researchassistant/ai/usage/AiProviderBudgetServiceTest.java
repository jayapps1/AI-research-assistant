package com.researchassistant.ai.usage;

import com.researchassistant.ai.exception.AiDevelopmentBudgetExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiProviderBudgetServiceTest {

    @Mock
    private AiUsageCostRepository costRepository;

    @Mock
    private AiProviderCostCalculator costCalculator;

    private AiProviderBudgetService budgetService;

    @BeforeEach
    void setUp() {
        budgetService = new AiProviderBudgetService(
                true,
                new BigDecimal("4.00"),
                costRepository,
                costCalculator
        );
    }

    @Test
    @DisplayName("Budget check passes when current spend + estimated call is below limit")
    void testWithinBudget() {
        when(costRepository.sumTotalCostByCurrency("USD")).thenReturn(new BigDecimal("1.50"));
        when(costCalculator.estimateCallCostUsd(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new BigDecimal("0.005000"));

        // Should not throw
        budgetService.checkBudgetBeforeCall("OPENAI", "gpt-5.6-luna", 2000, 4096);
    }

    @Test
    @DisplayName("Budget check throws AiDevelopmentBudgetExceededException when limit would be breached")
    void testExceedsBudgetThrows() {
        when(costRepository.sumTotalCostByCurrency("USD")).thenReturn(new BigDecimal("3.998000"));
        when(costCalculator.estimateCallCostUsd(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new BigDecimal("0.005000"));

        assertThatThrownBy(() -> budgetService.checkBudgetBeforeCall("OPENAI", "gpt-5.6-luna", 2000, 4096))
                .isInstanceOf(AiDevelopmentBudgetExceededException.class)
                .hasMessageContaining("The development AI budget ($4.00) has been reached");
    }

    @Test
    @DisplayName("Reports correct threshold statuses: NORMAL, WARNING_75, WARNING_90, EXCEEDED")
    void testThresholdStatuses() {
        when(costRepository.sumTotalCostByCurrency("USD")).thenReturn(new BigDecimal("1.00"));
        assertThat(budgetService.getBudgetStatus().thresholdStatus()).isEqualTo("NORMAL");

        when(costRepository.sumTotalCostByCurrency("USD")).thenReturn(new BigDecimal("3.00")); // 75%
        assertThat(budgetService.getBudgetStatus().thresholdStatus()).isEqualTo("WARNING_75");

        when(costRepository.sumTotalCostByCurrency("USD")).thenReturn(new BigDecimal("3.65")); // >90%
        assertThat(budgetService.getBudgetStatus().thresholdStatus()).isEqualTo("WARNING_90");

        when(costRepository.sumTotalCostByCurrency("USD")).thenReturn(new BigDecimal("4.01")); // >=100%
        assertThat(budgetService.getBudgetStatus().thresholdStatus()).isEqualTo("EXCEEDED");
    }
}
