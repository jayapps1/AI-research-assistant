package com.researchassistant.ai.exception;

import java.math.BigDecimal;

public class AiDevelopmentBudgetExceededException extends RuntimeException {

    private final BigDecimal currentSpendUsd;
    private final BigDecimal budgetLimitUsd;
    private final BigDecimal estimatedCallCostUsd;

    public AiDevelopmentBudgetExceededException(
            BigDecimal currentSpendUsd,
            BigDecimal budgetLimitUsd,
            BigDecimal estimatedCallCostUsd
    ) {
        super(String.format(
                "The development AI budget ($%.2f) has been reached (current spend: $%.4f, estimated call: $%.4f). " +
                "Real provider calls are temporarily paused to protect prepaid funds. Please contact administrator.",
                budgetLimitUsd != null ? budgetLimitUsd.doubleValue() : 4.00,
                currentSpendUsd != null ? currentSpendUsd.doubleValue() : 0.0,
                estimatedCallCostUsd != null ? estimatedCallCostUsd.doubleValue() : 0.0
        ));
        this.currentSpendUsd = currentSpendUsd;
        this.budgetLimitUsd = budgetLimitUsd;
        this.estimatedCallCostUsd = estimatedCallCostUsd;
    }

    public BigDecimal getCurrentSpendUsd() {
        return currentSpendUsd;
    }

    public BigDecimal getBudgetLimitUsd() {
        return budgetLimitUsd;
    }

    public BigDecimal getEstimatedCallCostUsd() {
        return estimatedCallCostUsd;
    }
}
