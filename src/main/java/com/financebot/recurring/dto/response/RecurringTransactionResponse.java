package com.financebot.recurring.dto.response;

import com.financebot.recurring.domain.RecurrenceFrequency;
import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record RecurringTransactionResponse(
        Long id,
        String description,
        BigDecimal amount,
        TransactionType type,
        SourceType sourceType,
        RecurrenceFrequency frequency,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate nextExecutionDate,
        boolean active,
        LocalDateTime lastExecutedAt,
        Long accountId,
        Long categoryId
) {
}