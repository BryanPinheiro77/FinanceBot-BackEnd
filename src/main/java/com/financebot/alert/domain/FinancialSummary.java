package com.financebot.alert.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialSummary(
        String periodType,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal income,
        BigDecimal expense,
        BigDecimal balance,
        String explanation
) {
}
