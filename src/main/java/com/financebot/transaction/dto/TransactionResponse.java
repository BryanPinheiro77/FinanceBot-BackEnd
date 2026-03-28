package com.financebot.transaction.dto;

import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        BigDecimal amount,
        String description,
        LocalDate date,
        TransactionType type,
        SourceType sourceType,
        Long accountId,
        String accountName,
        Long categoryId,
        String categoryName,
        LocalDateTime createdAt
) {
}