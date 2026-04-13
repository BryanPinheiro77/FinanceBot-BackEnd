package com.financebot.telegrambot.dto.request;

import java.math.BigDecimal;

public record UpdateMonthlyBaseIncomeRequest(
        BigDecimal monthlyBaseIncome
) {
}