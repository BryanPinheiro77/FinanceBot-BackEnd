package com.financebot.transaction.dto.request;

import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstallmentAlertPreviewRequest(
        @NotNull
        @DecimalMin("0.01")
        BigDecimal totalAmount,

        @NotNull
        LocalDate firstInstallmentDate,

        @NotNull
        TransactionType type,

        @NotNull
        SourceType sourceType,

        @NotNull
        Long accountId,

        @NotNull
        Long categoryId,

        @NotNull
        @Min(2)
        Integer totalInstallments
) {
}