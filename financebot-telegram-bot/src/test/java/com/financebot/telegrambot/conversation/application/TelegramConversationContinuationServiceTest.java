package com.financebot.telegrambot.conversation.application;

import com.financebot.telegrambot.conversation.domain.TelegramConversationContext;
import com.financebot.telegrambot.conversation.domain.TelegramConversationContextType;
import com.financebot.telegrambot.conversation.domain.TelegramConversationMissingField;
import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.handler.TelegramTransactionPreviewHandler;
import com.financebot.telegrambot.intent.TelegramIntentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramConversationContinuationServiceTest {

    private static final Long TELEGRAM_ID = 123L;

    @Mock
    private TelegramConversationContextService telegramConversationContextService;

    @Mock
    private TelegramInstallmentDueDayResolver telegramInstallmentDueDayResolver;

    @Mock
    private TelegramTransactionPreviewHandler telegramTransactionPreviewHandler;

    private TelegramConversationContinuationService service;

    @BeforeEach
    void setUp() {
        service = new TelegramConversationContinuationService(
                telegramConversationContextService,
                telegramInstallmentDueDayResolver,
                telegramTransactionPreviewHandler
        );
    }

    @Test
    void shouldCheckIfThereIsPendingContext() {
        when(telegramConversationContextService.hasPendingContext(TELEGRAM_ID)).thenReturn(true);

        boolean result = service.hasPendingContext(TELEGRAM_ID);

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnMessageWhenPendingContextIsNotFound() {
        when(telegramConversationContextService.findPendingContext(TELEGRAM_ID))
                .thenReturn(Optional.empty());

        String result = service.handle(TELEGRAM_ID, "dia 15");

        assertThat(result).isEqualTo("Não encontrei uma conversa pendente. Envie sua mensagem novamente.");
        verify(telegramConversationContextService, never()).clearPendingContext(TELEGRAM_ID);
        verify(telegramTransactionPreviewHandler, never()).handlePreview(org.mockito.Mockito.any(), org.mockito.Mockito.any());
    }

    @Test
    void shouldClearInvalidContextWithoutMissingFields() {
        TelegramConversationContext context = new TelegramConversationContext(
                TelegramConversationContextType.PENDING_MISSING_INFORMATION,
                TelegramIntentType.CREATE_INSTALLMENT_EXPENSE,
                parsedMessage(null),
                "comprei um iPhone de 6000 em 10x",
                Set.of(),
                Instant.now()
        );
        when(telegramConversationContextService.findPendingContext(TELEGRAM_ID))
                .thenReturn(Optional.of(context));

        String result = service.handle(TELEGRAM_ID, "dia 15");

        assertThat(result).isEqualTo("Não consegui continuar essa conversa. Envie sua mensagem novamente.");
        verify(telegramConversationContextService).clearPendingContext(TELEGRAM_ID);
        verify(telegramTransactionPreviewHandler, never()).handlePreview(org.mockito.Mockito.any(), org.mockito.Mockito.any());
    }

    @Test
    void shouldCompleteInstallmentDueDayAndDelegateToPreview() {
        LocalDate dueDate = LocalDate.of(2026, 6, 15);
        TelegramConversationContext context = contextWithMissingField(
                TelegramConversationMissingField.INSTALLMENT_DUE_DAY
        );
        when(telegramConversationContextService.findPendingContext(TELEGRAM_ID))
                .thenReturn(Optional.of(context));
        when(telegramInstallmentDueDayResolver.resolve("dia 15"))
                .thenReturn(Optional.of(dueDate));
        when(telegramTransactionPreviewHandler.handlePreview(
                org.mockito.Mockito.eq(TELEGRAM_ID),
                org.mockito.Mockito.any(),
                org.mockito.Mockito.eq(false)
        ))
                .thenReturn("preview");

        String result = service.handle(TELEGRAM_ID, "dia 15");

        assertThat(result).isEqualTo("preview");
        verify(telegramConversationContextService).clearPendingContext(TELEGRAM_ID);

        ArgumentCaptor<ParsedTelegramMessage> parsedCaptor =
                ArgumentCaptor.forClass(ParsedTelegramMessage.class);
        verify(telegramTransactionPreviewHandler).handlePreview(
                org.mockito.Mockito.eq(TELEGRAM_ID),
                parsedCaptor.capture(),
                org.mockito.Mockito.eq(false)
        );

        ParsedTelegramMessage completedMessage = parsedCaptor.getValue();
        assertThat(completedMessage.date()).isEqualTo(dueDate);
        assertThat(completedMessage.intentType()).isEqualTo(TelegramIntentType.CREATE_INSTALLMENT_EXPENSE);
        assertThat(completedMessage.description()).isEqualTo("iPhone");
        assertThat(completedMessage.totalInstallments()).isEqualTo(10);
    }

    @Test
    void shouldKeepContextWhenInstallmentDueDayCannotBeResolved() {
        TelegramConversationContext context = contextWithMissingField(
                TelegramConversationMissingField.INSTALLMENT_DUE_DAY
        );
        when(telegramConversationContextService.findPendingContext(TELEGRAM_ID))
                .thenReturn(Optional.of(context));
        when(telegramInstallmentDueDayResolver.resolve("na proxima fatura"))
                .thenReturn(Optional.empty());

        String result = service.handle(TELEGRAM_ID, "na proxima fatura");

        assertThat(result).contains("Não consegui identificar o dia de vencimento.");
        verify(telegramConversationContextService, never()).clearPendingContext(TELEGRAM_ID);
        verify(telegramTransactionPreviewHandler, never()).handlePreview(org.mockito.Mockito.any(), org.mockito.Mockito.any());
    }

    @Test
    void shouldReturnUnsupportedMessageForUnhandledMissingField() {
        TelegramConversationContext context = contextWithMissingField(TelegramConversationMissingField.ACCOUNT);
        when(telegramConversationContextService.findPendingContext(TELEGRAM_ID))
                .thenReturn(Optional.of(context));

        String result = service.handle(TELEGRAM_ID, "nubank");

        assertThat(result).contains("Ainda não consigo completar essa informação pendente.");
        verify(telegramConversationContextService, never()).clearPendingContext(TELEGRAM_ID);
        verify(telegramTransactionPreviewHandler, never()).handlePreview(org.mockito.Mockito.any(), org.mockito.Mockito.any());
    }

    private TelegramConversationContext contextWithMissingField(
            TelegramConversationMissingField missingField
    ) {
        return new TelegramConversationContext(
                TelegramConversationContextType.PENDING_MISSING_INFORMATION,
                TelegramIntentType.CREATE_INSTALLMENT_EXPENSE,
                parsedMessage(null),
                "comprei um iPhone de 6000 em 10x",
                Set.of(missingField),
                Instant.now()
        );
    }

    private ParsedTelegramMessage parsedMessage(LocalDate date) {
        return new ParsedTelegramMessage(
                TelegramIntentType.CREATE_INSTALLMENT_EXPENSE,
                null,
                "iPhone",
                date,
                "comprei um iPhone de 6000 em 10x",
                null,
                null,
                null,
                null,
                10,
                null,
                null
        );
    }
}
