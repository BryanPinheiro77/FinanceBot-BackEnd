package com.financebot.telegrambot.dto;

import com.financebot.telegrambot.intent.TelegramIntentType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PendingTelegramTransaction(
        TelegramIntentType intentType,
        BigDecimal amount,
        String description,
        LocalDate date,
        String categoryName,
        String accountName,
        Integer totalInstallments,
        Integer firstRemainingInstallmentNumber,
        String originalMessage
) {

    public boolean isInstallment() {
        return intentType == TelegramIntentType.CREATE_INSTALLMENT_EXPENSE
                || intentType == TelegramIntentType.CREATE_EXISTING_INSTALLMENT_EXPENSE;
    }

    public boolean isExistingInstallment() {
        return intentType == TelegramIntentType.CREATE_EXISTING_INSTALLMENT_EXPENSE;
    }
}