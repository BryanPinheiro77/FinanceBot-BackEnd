package com.financebot.telegram.dto.request;

import java.math.BigDecimal;

public record InstallmentPurchaseCapacityRequest(
        Long telegramId,
        BigDecimal totalAmount,
        Integer totalInstallments
) {
}
