package com.financebot.analysis.dto.response;

import com.financebot.transaction.dto.response.InstallmentTransactionResponse;

public record InstallmentTransactionCreationResponse(
        InstallmentTransactionResponse installment,
        FinancialCommitmentResponse analysis
) {
}