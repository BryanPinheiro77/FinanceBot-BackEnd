package com.financebot.telegrambot.parser;

import com.financebot.telegrambot.dto.ParsedDateRange;
import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.intent.TelegramIntentType;
import com.financebot.telegrambot.service.TelegramDateRangeResolver;
import com.financebot.telegrambot.service.TelegramIntentService;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TelegramQueryParser {

    private final TelegramIntentService support;
    private final TelegramDateRangeResolver dateRangeResolver;

    public TelegramQueryParser(
            TelegramIntentService support,
            TelegramDateRangeResolver dateRangeResolver
    ) {
        this.support = support;
        this.dateRangeResolver = dateRangeResolver;
    }

    public ParsedTelegramMessage parse(String normalizedText, String originalText) {
        if (support.isMonthAnalysisQuery(normalizedText)) {
            ParsedDateRange dateRange = dateRangeResolver.resolve(normalizedText);
            return message(TelegramIntentType.QUERY_MONTH_ANALYSIS, originalText,
                    dateRange.startDate(), dateRange.endDate(), null, null, null, null);
        }

        if (support.isInstallmentCountQuery(normalizedText)) {
            ParsedDateRange dateRange = dateRangeResolver.resolve(normalizedText);
            return message(TelegramIntentType.QUERY_INSTALLMENT_COUNT, originalText,
                    dateRange.startDate(), dateRange.endDate(), null, null, null, null);
        }

        if (support.isActiveInstallmentsQuery(normalizedText)) {
            return message(TelegramIntentType.QUERY_ACTIVE_INSTALLMENTS, originalText,
                    null, null, null, null, null, null);
        }

        if (support.isInstallmentRemainingQuery(normalizedText)) {
            return message(TelegramIntentType.QUERY_INSTALLMENT_REMAINING, originalText,
                    null, null, support.extractInstallmentQueryTarget(normalizedText), null, null, null);
        }

        if (support.isInstallmentEndDateQuery(normalizedText)) {
            return message(TelegramIntentType.QUERY_INSTALLMENT_END_DATE, originalText,
                    null, null, support.extractInstallmentQueryTarget(normalizedText), null, null, null);
        }

        if (support.isInstallmentPurchaseCapacityQuery(normalizedText)) {
            BigDecimal totalAmount = support.extractInstallmentPurchaseAmount(normalizedText);
            Integer totalInstallments = support.extractInstallmentCount(normalizedText);

            if (totalAmount != null && totalInstallments != null && totalInstallments >= 2) {
                return message(TelegramIntentType.QUERY_INSTALLMENT_PURCHASE_CAPACITY, originalText,
                        null, null, null, totalInstallments, totalAmount, null);
            }
        }

        if (support.isTransactionTotalQuery(normalizedText)) {
            ParsedDateRange dateRange = dateRangeResolver.resolve(normalizedText);
            return message(TelegramIntentType.QUERY_TRANSACTION_TOTAL, originalText,
                    dateRange.startDate(), dateRange.endDate(), null, null, null, null,
                    support.extractCategoryName(normalizedText), support.extractAccountName(normalizedText));
        }

        return null;
    }

    private ParsedTelegramMessage message(
            TelegramIntentType intentType,
            String originalText,
            LocalDate startDate,
            LocalDate endDate,
            String installmentQueryTarget,
            Integer totalInstallments,
            BigDecimal totalAmount,
            String unused
    ) {
        return message(intentType, originalText, startDate, endDate, installmentQueryTarget,
                totalInstallments, totalAmount, unused, null, null);
    }

    private ParsedTelegramMessage message(
            TelegramIntentType intentType,
            String originalText,
            LocalDate startDate,
            LocalDate endDate,
            String installmentQueryTarget,
            Integer totalInstallments,
            BigDecimal totalAmount,
            String unused,
            String categoryName,
            String accountName
    ) {
        return new ParsedTelegramMessage(
                intentType,
                null,
                null,
                LocalDate.now(),
                originalText,
                categoryName,
                accountName,
                startDate,
                endDate,
                totalInstallments,
                null,
                installmentQueryTarget,
                totalAmount,
                null
        );
    }
}
