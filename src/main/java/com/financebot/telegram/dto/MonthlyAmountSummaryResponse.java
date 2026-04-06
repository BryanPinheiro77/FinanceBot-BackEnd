package com.financebot.telegram.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyAmountSummaryResponse(
        String type,
        YearMonth referenceMonth,
        BigDecimal totalAmount
) {
}