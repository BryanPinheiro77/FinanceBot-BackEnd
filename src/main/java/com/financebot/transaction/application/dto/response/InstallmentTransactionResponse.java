package com.financebot.transaction.application.dto.response;

import java.util.List;

public record InstallmentTransactionResponse(
        String installmentGroupId,
        Integer totalInstallments,
        List<TransactionResponse> transactions
) {
}