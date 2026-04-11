package com.financebot.telegrambot.dto;

import java.time.LocalDate;

public record TelegramInstallmentCountRequest(
        Long telegramId,
        LocalDate startDate,
        LocalDate endDate
) {
}