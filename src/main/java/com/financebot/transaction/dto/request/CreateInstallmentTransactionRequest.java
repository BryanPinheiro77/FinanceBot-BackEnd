package com.financebot.transaction.dto.request;

import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateInstallmentTransactionRequest(

        @NotNull(message = "Total amount is required")
        @DecimalMin(value = "0.01", message = "Total amount must be greater than zero")
        BigDecimal totalAmount,

        @NotBlank(message = "Description is required")
        @Size(max = 255, message = "Description must have at most 255 characters")
        String description,

        @NotNull(message = "First installment date is required")
        LocalDate firstInstallmentDate,

        @NotNull(message = "Type is required")
        TransactionType type,

        @NotNull(message = "Source type is required")
        SourceType sourceType,

        @NotNull(message = "Account id is required")
        Long accountId,

        @NotNull(message = "Category id is required")
        Long categoryId,

        @NotNull(message = "Total installments is required")
        @Min(value = 2, message = "Total installments must be at least 2")
        Integer totalInstallments
) {
}