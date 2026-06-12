package com.financebot.telegrambot.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateExistingInstallmentTransactionFromTelegramRequest(
        Long telegramId,
        BigDecimal totalAmount,
        String description,
        LocalDate firstRemainingInstallmentDate,
        String accountName,
        String categoryName,
        Integer totalInstallments,
        Integer firstRemainingInstallmentNumber
) {
}
