package com.financebot.telegram.dto.request;

import java.time.LocalDate;

public record TelegramInstallmentCountRequest(
        Long telegramId,
        LocalDate startDate,
        LocalDate endDate
) {
}