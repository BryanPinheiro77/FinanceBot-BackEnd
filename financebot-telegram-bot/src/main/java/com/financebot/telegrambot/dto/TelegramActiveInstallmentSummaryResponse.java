package com.financebot.telegrambot.dto;

import java.time.LocalDate;

public record TelegramActiveInstallmentSummaryResponse(
        boolean hasActiveInstallment,
        String installmentGroupId,
        String description,
        Integer nextInstallmentNumber,
        Integer totalInstallments,
        Integer remainingInstallments,
        LocalDate endDate
) {
}
