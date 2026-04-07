package com.financebot.telegram.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TelegramTransactionSummaryResponse(
        String type,
        String categoryName,
        String accountName,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalAmount
) {
}