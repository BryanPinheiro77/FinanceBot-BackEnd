package com.financebot.recurring.dto.request;

import com.financebot.recurring.domain.RecurrenceFrequency;
import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateRecurringTransactionRequest(
        @NotBlank
        @Size(max = 255)
        String description,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal amount,

        @NotNull
        TransactionType type,

        @NotNull
        SourceType sourceType,

        @NotNull
        RecurrenceFrequency frequency,

        @NotNull
        LocalDate startDate,

        LocalDate endDate,

        @NotNull
        Long accountId,

        @NotNull
        Long categoryId,

        @NotNull
        Boolean active
) {
}