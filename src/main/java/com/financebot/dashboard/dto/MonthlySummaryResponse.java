package com.financebot.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public record MonthlySummaryResponse(
        YearMonth referenceMonth,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance
) {
}