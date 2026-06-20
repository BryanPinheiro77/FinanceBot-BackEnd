package com.financebot.transaction.application.dto.request;

import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.TransactionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateExistingInstallmentTransactionRequest(
        @DecimalMin(value = "0.01", message = "Total amount must be greater than zero")
        BigDecimal totalAmount,

        @Positive(message = "Monthly amount must be positive")
        BigDecimal monthlyAmount,

        @NotBlank(message = "Description is required")
        @Size(max = 255, message = "Description must have at most 255 characters")
        String description,

        @NotNull(message = "First remaining installment date is required")
        LocalDate firstRemainingInstallmentDate,

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
        Integer totalInstallments,

        @NotNull(message = "First remaining installment number is required")
        @Min(value = 1, message = "First remaining installment number must be at least 1")
        Integer firstRemainingInstallmentNumber
) {

    @AssertTrue(message = "Exactly one of total amount or monthly amount must be provided")
    public boolean isAmountSelectionValid() {
        return (totalAmount != null) ^ (monthlyAmount != null);
    }
}
