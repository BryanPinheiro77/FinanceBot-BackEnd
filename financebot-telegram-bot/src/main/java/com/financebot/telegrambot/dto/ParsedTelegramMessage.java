package com.financebot.telegrambot.dto;

import com.financebot.telegrambot.intent.TelegramIntentType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParsedTelegramMessage(
        TelegramIntentType intentType,
        BigDecimal amount,
        String description,
        LocalDate date,
        String originalMessage,
        String categoryName,
        String accountName,
        LocalDate startDate,
        LocalDate endDate,
        Integer totalInstallments
) {
}
