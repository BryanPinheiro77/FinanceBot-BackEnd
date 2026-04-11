package com.financebot.telegram.dto;

import java.math.BigDecimal;

public record MonthlyAmountSummaryResponse(
        String type,
        String referenceMonth,
        BigDecimal totalAmount
) {
}