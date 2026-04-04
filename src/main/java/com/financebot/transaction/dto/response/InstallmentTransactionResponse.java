package com.financebot.transaction.dto.response;

import java.util.List;

public record InstallmentTransactionResponse(
        String installmentGroupId,
        Integer totalInstallments,
        List<TransactionResponse> transactions
) {
}