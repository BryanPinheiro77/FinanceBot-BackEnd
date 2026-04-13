package com.financebot.telegrambot.dto.response;

import java.math.BigDecimal;

public record MonthlyAmountSummaryResponse(
        String type,
        String referenceMonth,
        BigDecimal totalAmount
) {
}