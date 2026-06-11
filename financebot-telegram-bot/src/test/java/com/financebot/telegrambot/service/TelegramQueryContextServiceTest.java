package com.financebot.telegrambot.service;

import com.financebot.telegrambot.conversation.application.port.out.TelegramQueryContextStore;
import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.intent.TelegramIntentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramQueryContextServiceTest {

    @Mock
    private TelegramIntentService telegramIntentService;

    @Mock
    private TelegramQueryContextStore telegramQueryContextStore;

    private TelegramQueryContextService service;

    @BeforeEach
    void setUp() {
        service = new TelegramQueryContextService(
                telegramIntentService,
                new TelegramDateRangeResolver(),
                new TelegramNaturalLanguageVocabulary(),
                telegramQueryContextStore
        );
    }

    @Test
    void shouldApplyPreviousTransactionQueryContextToContinuation() {
        Long telegramId = 123L;
        ParsedTelegramMessage currentParse = unknown("e mês passado?");
        ParsedTelegramMessage previous = transactionTotalContext();
        when(telegramQueryContextStore.findByTelegramId(telegramId))
                .thenReturn(Optional.of(previous));

        ParsedTelegramMessage result = service.applyQueryContext(
                telegramId,
                "e mês passado?",
                currentParse
        );

        assertThat(result.intentType()).isEqualTo(TelegramIntentType.QUERY_TRANSACTION_TOTAL);
        assertThat(result.categoryName()).isEqualTo("Alimentação");
        assertThat(result.startDate()).isEqualTo(LocalDate.now().minusMonths(1).withDayOfMonth(1));
        assertThat(result.endDate()).isEqualTo(LocalDate.now().withDayOfMonth(1).minusDays(1));
    }

    @Test
    void shouldReturnCurrentParseWhenMessageIsNotContinuation() {
        ParsedTelegramMessage currentParse = unknown("mercado");

        ParsedTelegramMessage result = service.applyQueryContext(123L, "mercado", currentParse);

        assertThat(result).isEqualTo(currentParse);
        verify(telegramQueryContextStore, never()).findByTelegramId(123L);
    }

    @Test
    void shouldReturnCurrentParseWhenThereIsNoPreviousContext() {
        Long telegramId = 123L;
        ParsedTelegramMessage currentParse = unknown("e mês passado?");
        when(telegramQueryContextStore.findByTelegramId(telegramId)).thenReturn(Optional.empty());

        ParsedTelegramMessage result = service.applyQueryContext(
                telegramId,
                "e mês passado?",
                currentParse
        );

        assertThat(result).isEqualTo(currentParse);
    }

    @Test
    void shouldSaveSupportedQueryContext() {
        ParsedTelegramMessage parsedMessage = transactionTotalContext();

        service.saveQueryContext(123L, parsedMessage);

        verify(telegramQueryContextStore).save(123L, parsedMessage);
    }

    @Test
    void shouldNotSaveUnsupportedQueryContext() {
        ParsedTelegramMessage parsedMessage = new ParsedTelegramMessage(
                TelegramIntentType.CREATE_EXPENSE,
                null,
                null,
                LocalDate.now(),
                "gastei 50 no mercado",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        service.saveQueryContext(123L, parsedMessage);

        verify(telegramQueryContextStore, never()).save(123L, parsedMessage);
    }

    private ParsedTelegramMessage transactionTotalContext() {
        return new ParsedTelegramMessage(
                TelegramIntentType.QUERY_TRANSACTION_TOTAL,
                null,
                null,
                LocalDate.now(),
                "quanto gastei com alimentação esse mês?",
                "Alimentação",
                null,
                LocalDate.now().withDayOfMonth(1),
                LocalDate.now(),
                null,
                null,
                null,
                null
        );
    }

    private ParsedTelegramMessage unknown(String originalMessage) {
        return new ParsedTelegramMessage(
                TelegramIntentType.UNKNOWN,
                null,
                null,
                null,
                originalMessage,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
