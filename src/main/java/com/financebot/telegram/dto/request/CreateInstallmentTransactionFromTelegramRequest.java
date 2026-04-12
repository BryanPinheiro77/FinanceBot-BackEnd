package com.financebot.telegram.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateInstallmentTransactionFromTelegramRequest(
        Long telegramId,
        BigDecimal totalAmount,
        String description,
        LocalDate firstInstallmentDate,
        String accountName,
        String categoryName,
        Integer totalInstallments
) {
}
