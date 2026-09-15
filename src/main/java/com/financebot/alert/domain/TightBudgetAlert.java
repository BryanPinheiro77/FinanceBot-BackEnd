package com.financebot.alert.domain;

import java.math.BigDecimal;

public record TightBudgetAlert(
        BigDecimal commitmentPercentage,
        BigDecimal projectedNet,
        String explanation
) {
}
