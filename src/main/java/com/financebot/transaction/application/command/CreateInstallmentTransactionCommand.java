package com.financebot.transaction.application.command;

import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.user.domain.User;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateInstallmentTransactionCommand(
        BigDecimal totalAmount,
        String description,
        LocalDate firstInstallmentDate,
        TransactionType type,
        SourceType sourceType,
        Long accountId,
        Long categoryId,
        Integer totalInstallments,
        User user
) {
}