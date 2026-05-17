package com.financebot.transaction.domain.installment;

import java.util.List;

public record InstallmentPlan(
        String installmentGroupId,
        Integer totalInstallments,
        List<InstallmentPlanItem> items
) {
}