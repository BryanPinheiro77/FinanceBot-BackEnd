package com.financebot.transaction.application.dto.request;

import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionAlertPreviewRequest(
        @NotNull
        @DecimalMin("0.01")
        BigDecimal amount,

        @NotNull
        LocalDate date,

        @NotNull
        TransactionType type,

        @NotNull
        SourceType sourceType,

        @NotNull
        Long accountId,

        @NotNull
        Long categoryId
) {
}