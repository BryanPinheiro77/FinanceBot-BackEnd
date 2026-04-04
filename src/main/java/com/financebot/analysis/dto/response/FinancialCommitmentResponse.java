package com.financebot.analysis.dto.response;

import java.math.BigDecimal;

public record FinancialCommitmentResponse(
        BigDecimal totalFutureInstallments,
        BigDecimal nextMonthProjectedExpense,
        BigDecimal monthlyIncomeReference,
        BigDecimal commitmentPercentage,
        Long activeInstallmentCount,
        boolean excessiveInstallments,
        boolean tightBudgetRisk,
        boolean riskDetected,
        String riskLevel,
        String message
) {
}