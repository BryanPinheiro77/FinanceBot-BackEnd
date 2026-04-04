package com.financebot.analysis.dto.response;

import com.financebot.transaction.dto.response.TransactionResponse;

public record TransactionCreationResponse(
        TransactionResponse transaction,
        FinancialCommitmentResponse analysis
) {
}
