package com.financebot.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MonthlySummaryResponse(String month, LocalDate startDate, LocalDate endDate,
                                     BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal balance,
                                     long transactionCount, List<DailySummaryResponse> daily,
                                     List<CategorySummaryResponse> expensesByCategory) {
}
