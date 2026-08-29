package com.financebot.telegram.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTelegramInstallmentCommand(
        Long telegramId,
        BigDecimal totalAmount,
        String description,
        LocalDate firstInstallmentDate,
        String accountName,
        String categoryName,
        Integer totalInstallments
) {
}
