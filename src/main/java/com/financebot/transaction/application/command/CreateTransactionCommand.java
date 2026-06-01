package com.financebot.transaction.application.command;

import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.user.domain.User;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionCommand(
        BigDecimal amount,
        String description,
        LocalDate date,
        TransactionType type,
        SourceType sourceType,
        Long accountId,
        Long categoryId,
        User user
) {
}