package com.financebot.telegrambot.parser;

import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.intent.TelegramIntentType;
import com.financebot.telegrambot.service.TelegramIntentService;

import java.math.BigDecimal;

public class TelegramTransactionParser {

    private final TelegramIntentService support;

    public TelegramTransactionParser(TelegramIntentService support) {
        this.support = support;
    }

    public ParsedTelegramMessage parse(String normalizedText, String originalText) {
        if (support.looksLikeExistingInstallmentExpense(normalizedText)) {
            BigDecimal monthlyAmount = support.extractMonthlyInstallmentAmount(normalizedText);
            BigDecimal totalAmount = monthlyAmount != null
                    ? null
                    : support.extractInstallmentPurchaseAmount(normalizedText);

            return new ParsedTelegramMessage(
                    TelegramIntentType.CREATE_EXISTING_INSTALLMENT_EXPENSE,
                    null,
                    support.extractInstallmentDescription(normalizedText),
                    support.extractDate(normalizedText),
                    originalText,
                    support.extractCategoryName(normalizedText),
                    support.extractAccountName(normalizedText),
                    null,
                    null,
                    support.extractInstallmentCount(normalizedText),
                    support.extractFirstRemainingInstallmentNumber(normalizedText),
                    null,
                    totalAmount,
                    monthlyAmount
            );
        }

        if (support.looksLikeInstallmentExpense(normalizedText)) {
            return new ParsedTelegramMessage(
                    TelegramIntentType.CREATE_INSTALLMENT_EXPENSE,
                    support.extractAmount(normalizedText),
                    support.extractInstallmentDescription(normalizedText),
                    support.extractDate(normalizedText),
                    originalText,
                    support.extractCategoryName(normalizedText),
                    support.extractAccountName(normalizedText),
                    null,
                    null,
                    support.extractInstallmentCount(normalizedText),
                    null,
                    null,
                    null,
                    null
            );
        }

        if (support.looksLikeExpense(normalizedText)) {
            return new ParsedTelegramMessage(
                    TelegramIntentType.CREATE_EXPENSE,
                    support.extractAmount(normalizedText),
                    support.extractDescriptionForTransaction(normalizedText),
                    support.extractDate(normalizedText),
                    originalText,
                    support.extractCategoryName(normalizedText),
                    support.extractAccountName(normalizedText),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        if (support.looksLikeIncome(normalizedText)) {
            return new ParsedTelegramMessage(
                    TelegramIntentType.CREATE_INCOME,
                    support.extractAmount(normalizedText),
                    support.extractDescriptionForTransaction(normalizedText),
                    support.extractDate(normalizedText),
                    originalText,
                    support.extractCategoryName(normalizedText),
                    support.extractAccountName(normalizedText),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        return null;
    }
}
