package com.financebot.transaction.application.command;

import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.user.domain.User;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateExistingInstallmentTransactionCommand(
        BigDecimal totalAmount,
        String description,
        LocalDate firstRemainingInstallmentDate,
        TransactionType type,
        SourceType sourceType,
        Long accountId,
        Long categoryId,
        Integer totalInstallments,
        Integer firstRemainingInstallmentNumber,
        User user
) {
}
