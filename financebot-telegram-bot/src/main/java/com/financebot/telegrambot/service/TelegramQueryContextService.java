package com.financebot.telegrambot.service;

import com.financebot.telegrambot.conversation.application.port.out.TelegramQueryContextStore;
import com.financebot.telegrambot.dto.ParsedDateRange;
import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.intent.TelegramIntentType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class TelegramQueryContextService {

    private final TelegramIntentService telegramIntentService;
    private final TelegramDateRangeResolver telegramDateRangeResolver;
    private final TelegramNaturalLanguageVocabulary telegramNaturalLanguageVocabulary;
    private final TelegramQueryContextStore telegramQueryContextStore;

    public TelegramQueryContextService(
            TelegramIntentService telegramIntentService,
            TelegramDateRangeResolver telegramDateRangeResolver,
            TelegramNaturalLanguageVocabulary telegramNaturalLanguageVocabulary,
            TelegramQueryContextStore telegramQueryContextStore
    ) {
        this.telegramIntentService = telegramIntentService;
        this.telegramDateRangeResolver = telegramDateRangeResolver;
        this.telegramNaturalLanguageVocabulary = telegramNaturalLanguageVocabulary;
        this.telegramQueryContextStore = telegramQueryContextStore;
    }

    public ParsedTelegramMessage applyQueryContext(
            Long telegramId,
            String messageText,
            ParsedTelegramMessage currentParse
    ) {
        if (currentParse.intentType() != TelegramIntentType.UNKNOWN || !looksLikeContinuation(messageText)) {
            return currentParse;
        }

        ParsedTelegramMessage previous = telegramQueryContextStore
                .findByTelegramId(telegramId)
                .orElse(null);
        if (previous == null) {
            return currentParse;
        }

        ParsedTelegramMessage merged = switch (previous.intentType()) {
            case QUERY_TRANSACTION_TOTAL -> mergeTransactionSummaryContext(previous, messageText);
            case QUERY_INSTALLMENT_REMAINING, QUERY_INSTALLMENT_END_DATE ->
                    mergeInstallmentQueryContext(previous, messageText);
            default -> null;
        };

        return merged != null ? merged : currentParse;
    }

    public void saveQueryContext(Long telegramId, ParsedTelegramMessage parsedMessage) {
        if (parsedMessage == null || !supportsContinuation(parsedMessage.intentType())) {
            return;
        }

        telegramQueryContextStore.save(telegramId, parsedMessage);
    }

    private ParsedTelegramMessage mergeTransactionSummaryContext(
            ParsedTelegramMessage previous,
            String messageText
    ) {
        String normalized = telegramNaturalLanguageVocabulary.normalize(messageText);
        boolean hasExplicitDateRange = telegramDateRangeResolver.hasExplicitRangeHint(normalized);
        String categoryName = telegramIntentService.extractCategoryName(messageText);
        String accountName = telegramIntentService.extractAccountName(messageText);

        if (!hasExplicitDateRange && categoryName == null && accountName == null) {
            return null;
        }

        LocalDate startDate = previous.startDate();
        LocalDate endDate = previous.endDate();

        if (hasExplicitDateRange) {
            ParsedDateRange dateRange = telegramDateRangeResolver.resolve(normalized);
            startDate = dateRange.startDate();
            endDate = dateRange.endDate();
        }

        return new ParsedTelegramMessage(
                previous.intentType(),
                null,
                null,
                LocalDate.now(),
                previous.originalMessage(),
                categoryName != null ? categoryName : previous.categoryName(),
                accountName != null ? accountName : previous.accountName(),
                startDate,
                endDate,
                null,
                previous.firstRemainingInstallmentNumber(),
                null,
                null
        );
    }

    private ParsedTelegramMessage mergeInstallmentQueryContext(
            ParsedTelegramMessage previous,
            String messageText
    ) {
        String target = telegramIntentService.extractInstallmentQueryTarget(messageText);

        if (target == null || target.isBlank()) {
            return null;
        }

        return new ParsedTelegramMessage(
                previous.intentType(),
                null,
                null,
                LocalDate.now(),
                previous.originalMessage(),
                null,
                null,
                null,
                null,
                null,
                previous.firstRemainingInstallmentNumber(),
                target,
                null
        );
    }

    private boolean looksLikeContinuation(String messageText) {
        String normalized = telegramNaturalLanguageVocabulary.normalize(messageText);
        return normalized.equals("e") || normalized.startsWith("e ");
    }

    private boolean supportsContinuation(TelegramIntentType intentType) {
        return intentType == TelegramIntentType.QUERY_TRANSACTION_TOTAL
                || intentType == TelegramIntentType.QUERY_INSTALLMENT_REMAINING
                || intentType == TelegramIntentType.QUERY_INSTALLMENT_END_DATE;
    }
}
