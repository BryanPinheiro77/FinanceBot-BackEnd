package com.financebot.user.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateMonthlyBaseIncomeRequest(
        @NotNull
        @DecimalMin("0.01")
        BigDecimal monthlyBaseIncome
) {
}