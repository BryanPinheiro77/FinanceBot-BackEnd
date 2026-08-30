package com.financebot.telegrambot.ai.application.service;

import com.financebot.telegrambot.ai.application.model.AiInterpretation;
import com.financebot.telegrambot.intent.TelegramIntentType;

import java.math.BigDecimal;
import java.util.Set;

public final class AiInterpretationValidator {

    private static final Set<TelegramIntentType> SUPPORTED_INTENTS = Set.of(
            TelegramIntentType.CREATE_EXPENSE,
            TelegramIntentType.CREATE_INCOME,
            TelegramIntentType.CREATE_INSTALLMENT_EXPENSE,
            TelegramIntentType.CREATE_EXISTING_INSTALLMENT_EXPENSE,
            TelegramIntentType.QUERY_MONTH_EXPENSE_TOTAL,
            TelegramIntentType.QUERY_MONTH_INCOME_TOTAL,
            TelegramIntentType.QUERY_MONTH_ANALYSIS,
            TelegramIntentType.QUERY_TRANSACTION_TOTAL,
            TelegramIntentType.QUERY_INSTALLMENT_COUNT,
            TelegramIntentType.QUERY_INSTALLMENT_PURCHASE_CAPACITY,
            TelegramIntentType.QUERY_INSTALLMENT_REMAINING,
            TelegramIntentType.QUERY_ACTIVE_INSTALLMENTS,
            TelegramIntentType.QUERY_INSTALLMENT_END_DATE
    );

    private AiInterpretationValidator() {
    }

    public static boolean isValid(AiInterpretation interpretation) {
        if (interpretation == null || !SUPPORTED_INTENTS.contains(interpretation.intentType())) {
            return false;
        }

        if (!validAmount(interpretation.amount())
                || !validAmount(interpretation.totalAmount())
                || !validAmount(interpretation.monthlyAmount())) {
            return false;
        }

        Integer installments = interpretation.totalInstallments();
        if (installments != null && (installments < 2 || installments > 120)) {
            return false;
        }

        Integer firstRemaining = interpretation.firstRemainingInstallmentNumber();
        if (firstRemaining != null && (firstRemaining < 1 || installments == null || firstRemaining > installments)) {
            return false;
        }

        if (interpretation.intentType() == TelegramIntentType.CREATE_EXPENSE
                || interpretation.intentType() == TelegramIntentType.CREATE_INCOME) {
            return interpretation.amount() != null && interpretation.description() != null
                    && !interpretation.description().isBlank();
        }

        if (interpretation.intentType() == TelegramIntentType.CREATE_INSTALLMENT_EXPENSE
                || interpretation.intentType() == TelegramIntentType.CREATE_EXISTING_INSTALLMENT_EXPENSE) {
            return installments != null
                    && (interpretation.totalAmount() != null || interpretation.monthlyAmount() != null)
                    && !(interpretation.totalAmount() != null && interpretation.monthlyAmount() != null);
        }

        if (interpretation.intentType() == TelegramIntentType.QUERY_INSTALLMENT_PURCHASE_CAPACITY) {
            return interpretation.totalAmount() != null && installments != null;
        }

        return true;
    }

    private static boolean validAmount(BigDecimal amount) {
        return amount == null || amount.compareTo(BigDecimal.ZERO) > 0;
    }
}
