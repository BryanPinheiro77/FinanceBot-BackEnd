package com.financebot.transaction.dto;

import java.util.List;

public record InstallmentTransactionResponse(
        String installmentGroupId,
        Integer totalInstallments,
        List<TransactionResponse> transactions
) {
}