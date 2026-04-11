package com.financebot.telegrambot.dto;

import java.math.BigDecimal;

public record InstallmentPurchaseCapacityRequest(
        Long telegramId,
        BigDecimal totalAmount,
        Integer totalInstallments
) {
}
