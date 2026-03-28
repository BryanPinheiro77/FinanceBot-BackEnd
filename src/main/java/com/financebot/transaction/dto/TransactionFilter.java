package com.financebot.transaction.dto;

import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.TransactionType;

import java.time.LocalDate;

public record TransactionFilter(
        TransactionType type,
        Long categoryId,
        Long accountId,
        LocalDate startDate,
        LocalDate endDate,
        SourceType sourceType,
        String description
) {
}