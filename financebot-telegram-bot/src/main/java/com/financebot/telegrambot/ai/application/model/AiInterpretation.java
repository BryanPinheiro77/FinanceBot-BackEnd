package com.financebot.telegrambot.ai.application.model;

import com.financebot.telegrambot.intent.TelegramIntentType;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Modelo interno; não expõe tipos do provedor de IA ao restante da aplicação. */
public record AiInterpretation(
        TelegramIntentType intentType,
        BigDecimal amount,
        BigDecimal totalAmount,
        BigDecimal monthlyAmount,
        String description,
        LocalDate date,
        String categoryName,
        String accountName,
        Integer totalInstallments,
        Integer firstRemainingInstallmentNumber,
        LocalDate startDate,
        LocalDate endDate
) {
}
