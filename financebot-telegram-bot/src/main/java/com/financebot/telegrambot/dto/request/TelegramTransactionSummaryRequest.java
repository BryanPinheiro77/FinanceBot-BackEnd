package com.financebot.telegrambot.dto.request;

import java.time.LocalDate;

public record TelegramTransactionSummaryRequest(
        Long telegramId,
        String type,
        String categoryName,
        String accountName,
        LocalDate startDate,
        LocalDate endDate
) {
}