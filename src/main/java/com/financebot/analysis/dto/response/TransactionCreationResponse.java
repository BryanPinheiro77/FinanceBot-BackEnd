package com.financebot.analysis.dto.response;

import com.financebot.transaction.application.dto.response.TransactionResponse;

public record TransactionCreationResponse(
        TransactionResponse transaction,
        FinancialCommitmentResponse analysis
) {
}
