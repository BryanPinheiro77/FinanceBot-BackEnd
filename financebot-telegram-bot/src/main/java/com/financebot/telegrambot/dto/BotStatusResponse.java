package com.financebot.telegrambot.dto;

import java.math.BigDecimal;

public record BotStatusResponse(
        boolean telegramLinked,
        BigDecimal monthlyBaseIncome,
        Integer activeRecurringTransactions,
        BigDecimal projectedNetNextMonth,
        String riskLevel
) {
}