package com.researchassistant.ai.usage;

import com.researchassistant.ai.exception.AiDevelopmentBudgetExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class AiProviderBudgetService {

    private static final Logger log = LoggerFactory.getLogger(AiProviderBudgetService.class);

    private final boolean budgetEnabled;
    private final BigDecimal budgetLimitUsd;
    private final AiUsageCostRepository costRepository;
    private final AiProviderCostCalculator costCalculator;

    public AiProviderBudgetService(
            @Value("${app.ai.development-budget.enabled:true}") boolean budgetEnabled,
            @Value("${app.ai.development-budget.limit-usd:4.00}") BigDecimal budgetLimitUsd,
            AiUsageCostRepository costRepository,
            AiProviderCostCalculator costCalculator
    ) {
        this.budgetEnabled = budgetEnabled;
        this.budgetLimitUsd = budgetLimitUsd != null ? budgetLimitUsd : new BigDecimal("4.00");
        this.costRepository = costRepository;
        this.costCalculator = costCalculator;
    }

    public record BudgetStatus(
            boolean enabled,
            BigDecimal limitUsd,
            BigDecimal currentSpendUsd,
            BigDecimal remainingUsd,
            BigDecimal spendPercentage,
            String thresholdStatus
    ) {}

    @Transactional(readOnly = true)
    public BudgetStatus getBudgetStatus() {
        BigDecimal currentSpend = costRepository.sumTotalCostByCurrency("USD");
        if (currentSpend == null) currentSpend = BigDecimal.ZERO;

        BigDecimal remaining = budgetLimitUsd.subtract(currentSpend).max(BigDecimal.ZERO);
        BigDecimal percentage = budgetLimitUsd.compareTo(BigDecimal.ZERO) > 0
                ? currentSpend.divide(budgetLimitUsd, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        String thresholdStatus = "NORMAL";
        if (currentSpend.compareTo(budgetLimitUsd) >= 0) {
            thresholdStatus = "EXCEEDED";
        } else if (percentage.compareTo(BigDecimal.valueOf(90)) >= 0) {
            thresholdStatus = "WARNING_90";
        } else if (percentage.compareTo(BigDecimal.valueOf(75)) >= 0) {
            thresholdStatus = "WARNING_75";
        }

        return new BudgetStatus(
                budgetEnabled,
                budgetLimitUsd,
                currentSpend,
                remaining,
                percentage,
                thresholdStatus
        );
    }

    @Transactional(readOnly = true)
    public void checkBudgetBeforeCall(String provider, String model, int estimatedInputTokens, int maxOutputTokens) {
        if (!budgetEnabled) {
            return;
        }

        BigDecimal currentSpend = costRepository.sumTotalCostByCurrency("USD");
        if (currentSpend == null) currentSpend = BigDecimal.ZERO;

        BigDecimal estimatedCost = costCalculator.estimateCallCostUsd(provider, model, estimatedInputTokens, maxOutputTokens);
        BigDecimal projectedSpend = currentSpend.add(estimatedCost);

        if (projectedSpend.compareTo(budgetLimitUsd) > 0) {
            log.warn("AI Development Budget Exceeded! Current spend: ${}, Estimated call: ${}, Budget ceiling: ${}",
                    currentSpend, estimatedCost, budgetLimitUsd);
            throw new AiDevelopmentBudgetExceededException(currentSpend, budgetLimitUsd, estimatedCost);
        }

        if (currentSpend.compareTo(budgetLimitUsd.multiply(new BigDecimal("0.90"))) >= 0) {
            log.warn("AI Development Budget Warning: spend is at or above 90% (Current: ${}, Limit: ${})",
                    currentSpend, budgetLimitUsd);
        } else if (currentSpend.compareTo(budgetLimitUsd.multiply(new BigDecimal("0.75"))) >= 0) {
            log.info("AI Development Budget Notice: spend is at or above 75% (Current: ${}, Limit: ${})",
                    currentSpend, budgetLimitUsd);
        }
    }
}
