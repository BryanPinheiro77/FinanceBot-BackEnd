package com.financebot.telegram.dto;

import java.time.LocalDate;

public record TelegramInstallmentCountRequest(
        Long telegramId,
        LocalDate startDate,
        LocalDate endDate
) {
}