package com.financebot.telegrambot.dto;

import java.math.BigDecimal;

public record UpdateMonthlyBaseIncomeRequest(
        BigDecimal monthlyBaseIncome
) {
}