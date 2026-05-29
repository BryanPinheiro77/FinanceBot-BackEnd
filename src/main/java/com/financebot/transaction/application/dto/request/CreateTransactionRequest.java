package com.financebot.transaction.application.dto.request;

import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Description is required")
        @Size(max = 255, message = "Description must have at most 255 characters")
        String description,

        @NotNull(message = "Date is required")
        LocalDate date,

        @NotNull(message = "Type is required")
        TransactionType type,

        @NotNull(message = "Source type is required")
        SourceType sourceType,

        @NotNull(message = "Account id is required")
        Long accountId,

        @NotNull(message = "Category id is required")
        Long categoryId
) {
}