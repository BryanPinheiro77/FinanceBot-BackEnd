package com.financebot.analysis.dto.response;

import java.math.BigDecimal;

public record InstallmentPurchaseCapacityResponse(
        BigDecimal totalAmount,
        Integer totalInstallments,
        BigDecimal estimatedInstallmentAmount,
        String analysisResult,
        String observation
) {
}
