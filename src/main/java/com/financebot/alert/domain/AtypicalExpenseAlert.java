package com.financebot.alert.domain;

import java.math.BigDecimal;
import java.time.YearMonth;

public record AtypicalExpenseAlert(
        String categoryName,
        YearMonth observedMonth,
        BigDecimal observedAmount,
        BigDecimal historicalAverage,
        BigDecimal variationPercentage,
        String explanation
) {
}
