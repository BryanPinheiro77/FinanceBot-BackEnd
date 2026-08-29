package com.financebot.telegram.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTelegramExistingInstallmentCommand(
        Long telegramId,
        BigDecimal totalAmount,
        BigDecimal monthlyAmount,
        String description,
        LocalDate firstRemainingInstallmentDate,
        String accountName,
        String categoryName,
        Integer totalInstallments,
        Integer firstRemainingInstallmentNumber
) {
}
