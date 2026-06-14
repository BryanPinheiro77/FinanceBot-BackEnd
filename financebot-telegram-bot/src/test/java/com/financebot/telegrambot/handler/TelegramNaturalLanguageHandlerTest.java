package com.financebot.telegrambot.handler;

import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.intent.TelegramIntentType;
import com.financebot.telegrambot.service.TelegramIntentService;
import com.financebot.telegrambot.service.TelegramQueryContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramNaturalLanguageHandlerTest {

    private static final Long TELEGRAM_ID = 123L;

    @Mock
    private TelegramIntentService telegramIntentService;

    @Mock
    private TelegramQueryContextService telegramQueryContextService;

    @Mock
    private TelegramFinancialQueryHandler telegramFinancialQueryHandler;

    @Mock
    private TelegramTransactionPreviewHandler telegramTransactionPreviewHandler;

    private TelegramNaturalLanguageHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TelegramNaturalLanguageHandler(
                telegramIntentService,
                telegramQueryContextService,
                telegramFinancialQueryHandler,
                telegramTransactionPreviewHandler
        );
    }

    @Test
    @DisplayName("deve enviar parcelamento existente para o preview")
    void shouldRouteExistingInstallmentToPreview() {
        String message = "comprei um celular de 3000 parcelado em 10x e ja paguei 5 parcelas";
        ParsedTelegramMessage parsedMessage = new ParsedTelegramMessage(
                TelegramIntentType.CREATE_EXISTING_INSTALLMENT_EXPENSE,
                null,
                "celular",
                LocalDate.of(2026, 6, 11),
                message,
                null,
                null,
                null,
                null,
                10,
                6,
                null,
                new BigDecimal("3000"),
                null
        );

        when(telegramIntentService.parse(message)).thenReturn(parsedMessage);
        when(telegramQueryContextService.applyQueryContext(TELEGRAM_ID, message, parsedMessage))
                .thenReturn(parsedMessage);
        when(telegramTransactionPreviewHandler.handlePreview(TELEGRAM_ID, parsedMessage))
                .thenReturn("preview");

        String result = handler.handle(message, TELEGRAM_ID);

        assertThat(result).isEqualTo("preview");
        verify(telegramTransactionPreviewHandler).handlePreview(TELEGRAM_ID, parsedMessage);
    }
}
