package com.financebot.telegrambot.dto;

import java.time.LocalDate;

public record TelegramInstallmentCountResponse(
        Long installmentCount,
        LocalDate startDate,
        LocalDate endDate
) {
}