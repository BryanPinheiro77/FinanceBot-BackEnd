package com.financebot.telegram.application.command;

import com.financebot.transaction.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTelegramTransactionCommand(
        Long telegramId,
        BigDecimal amount,
        String description,
        LocalDate date,
        TransactionType type,
        String categoryName,
        String accountName
) {
}
