package com.financebot.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailySummaryResponse(LocalDate date, BigDecimal totalIncome, BigDecimal totalExpense) {
}
