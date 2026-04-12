package com.financebot.telegram.dto.response;

import java.time.LocalDate;

public record TelegramInstallmentCountResponse(
        Long installmentCount,
        LocalDate startDate,
        LocalDate endDate
) {
}