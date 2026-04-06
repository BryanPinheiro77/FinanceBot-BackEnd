package com.financebot.telegrambot.dto;

import java.math.BigDecimal;

public record FinancialCommitmentResponse(
        BigDecimal monthlyBaseIncome,
        BigDecimal nextMonthProjectedExpense,
        BigDecimal projectedRecurringExpenseNextMonth,
        BigDecimal projectedRecurringIncomeNextMonth,
        BigDecimal nextMonthProjectedIncome,
        BigDecimal projectedNetNextMonth,
        BigDecimal commitmentPercentage,
        Integer activeInstallmentGroups,
        String riskLevel
) {
}