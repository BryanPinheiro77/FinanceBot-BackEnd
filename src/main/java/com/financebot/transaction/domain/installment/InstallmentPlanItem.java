package com.financebot.transaction.domain.installment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstallmentPlanItem(
        BigDecimal amount,
        String description,
        LocalDate date,
        Integer installmentNumber,
        Integer totalInstallments,
        String installmentGroupId
) {
}