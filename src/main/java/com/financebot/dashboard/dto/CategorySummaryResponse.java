package com.financebot.dashboard.dto;

import java.math.BigDecimal;

public record CategorySummaryResponse(Long categoryId, String categoryName, BigDecimal totalExpense) {
}
