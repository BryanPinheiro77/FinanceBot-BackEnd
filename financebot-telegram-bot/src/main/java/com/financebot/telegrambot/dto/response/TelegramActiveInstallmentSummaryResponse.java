package com.financebot.telegrambot.dto.response;

import java.time.LocalDate;

public record TelegramActiveInstallmentSummaryResponse(
        boolean hasActiveInstallment,
        String installmentGroupId,
        String description,
        LocalDate currentDueDate,
        Integer currentInstallmentNumber,
        LocalDate nextDueDate,
        Integer nextInstallmentNumber,
        Integer totalInstallments,
        Integer remainingInstallments,
        LocalDate endDate
) {
}
