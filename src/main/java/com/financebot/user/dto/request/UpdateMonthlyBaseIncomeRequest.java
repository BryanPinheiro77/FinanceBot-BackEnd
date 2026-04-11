package com.financebot.user.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateMonthlyBaseIncomeRequest(
        @NotNull(message = "A renda mensal base é obrigatória.")
        @DecimalMin(value = "0.01", message = "A renda mensal base deve ser maior que zero.")
        BigDecimal monthlyBaseIncome
) {
}