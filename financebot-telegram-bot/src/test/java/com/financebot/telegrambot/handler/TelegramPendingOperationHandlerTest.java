package com.financebot.telegrambot.handler;

import com.financebot.telegrambot.client.FinanceBotApiClient;
import com.financebot.telegrambot.conversation.application.TelegramConversationContextService;
import com.financebot.telegrambot.formatter.TelegramMessageFormatter;
import com.financebot.telegrambot.service.TelegramPendingConfirmationService;
import com.financebot.telegrambot.service.TelegramPendingQueryService;
import com.financebot.telegrambot.support.TelegramBotErrorMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramPendingOperationHandlerTest {

    private static final Long TELEGRAM_ID = 123L;

    @Mock
    private FinanceBotApiClient financeBotApiClient;

    @Mock
    private TelegramPendingConfirmationService telegramPendingConfirmationService;

    @Mock
    private TelegramPendingQueryService telegramPendingQueryService;

    @Mock
    private TelegramConversationContextService telegramConversationContextService;

    private TelegramPendingOperationHandler handler;

    @BeforeEach
    void setUp() {
        TelegramMessageFormatter formatter = new TelegramMessageFormatter();
        handler = new TelegramPendingOperationHandler(
                financeBotApiClient,
                telegramPendingConfirmationService,
                telegramPendingQueryService,
                formatter,
                new TelegramBotErrorMapper(formatter),
                telegramConversationContextService
        );
    }

    @Test
    void shouldCancelConversationContextWhenThereIsPendingContext() {
        when(telegramConversationContextService.hasPendingContext(TELEGRAM_ID)).thenReturn(true);

        String result = handler.handleCancellation(TELEGRAM_ID);

        assertThat(result).isEqualTo("❌ Operação cancelada com sucesso.");
        verify(telegramPendingConfirmationService).clearPending(TELEGRAM_ID);
        verify(telegramPendingQueryService).clearPending(TELEGRAM_ID);
        verify(telegramConversationContextService).clearPendingContext(TELEGRAM_ID);
    }

    @Test
    void shouldReturnMessageWhenThereIsNothingToCancel() {
        String result = handler.handleCancellation(TELEGRAM_ID);

        assertThat(result).isEqualTo("Não há nenhuma operação pendente para cancelar.");
        verify(telegramPendingConfirmationService, never()).clearPending(TELEGRAM_ID);
        verify(telegramPendingQueryService, never()).clearPending(TELEGRAM_ID);
        verify(telegramConversationContextService, never()).clearPendingContext(TELEGRAM_ID);
    }
}
