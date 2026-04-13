package com.financebot.telegrambot.dto.response;

import java.math.BigDecimal;

public record BotStatusResponse(
        boolean telegramLinked,
        BigDecimal monthlyBaseIncome,
        Integer activeRecurringTransactions,
        BigDecimal projectedNetNextMonth,
        String riskLevel
) {
}