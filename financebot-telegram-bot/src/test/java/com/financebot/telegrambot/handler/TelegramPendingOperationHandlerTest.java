package com.financebot.telegrambot.handler;

import com.financebot.telegrambot.client.FinanceBotApiClient;
import com.financebot.telegrambot.conversation.application.TelegramConversationContextService;
import com.financebot.telegrambot.dto.PendingTelegramTransaction;
import com.financebot.telegrambot.dto.request.CreateExistingInstallmentTransactionFromTelegramRequest;
import com.financebot.telegrambot.formatter.TelegramMessageFormatter;
import com.financebot.telegrambot.intent.TelegramIntentType;
import com.financebot.telegrambot.service.TelegramPendingConfirmationService;
import com.financebot.telegrambot.service.TelegramPendingQueryService;
import com.financebot.telegrambot.support.TelegramBotErrorMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
    @DisplayName("deve retornar mensagem quando nao ha operacao pendente para confirmar")
    void shouldReturnMessageWhenThereIsNoPendingConfirmation() {
        String result = handler.handleConfirmation(TELEGRAM_ID);

        assertThat(result).isEqualTo("Não há nenhuma operação pendente para confirmar.");
        verify(financeBotApiClient, never()).createTransaction(any());
        verify(financeBotApiClient, never()).createInstallmentTransaction(any());
        verify(financeBotApiClient, never()).createExistingInstallmentTransaction(any());
        verify(telegramPendingConfirmationService, never()).clearPending(TELEGRAM_ID);
    }

    @Test
    @DisplayName("deve confirmar parcelamento existente usando endpoint especifico")
    void shouldConfirmExistingInstallmentUsingSpecificEndpoint() {
        PendingTelegramTransaction pending = existingInstallmentPending();
        when(telegramPendingConfirmationService.getPending(TELEGRAM_ID)).thenReturn(pending);

        String result = handler.handleConfirmation(TELEGRAM_ID);

        ArgumentCaptor<CreateExistingInstallmentTransactionFromTelegramRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateExistingInstallmentTransactionFromTelegramRequest.class);

        verify(financeBotApiClient).createExistingInstallmentTransaction(requestCaptor.capture());
        verify(financeBotApiClient, never()).createInstallmentTransaction(any());
        verify(financeBotApiClient, never()).createTransaction(any());
        verify(telegramPendingConfirmationService).clearPending(TELEGRAM_ID);

        CreateExistingInstallmentTransactionFromTelegramRequest request = requestCaptor.getValue();
        assertThat(request.telegramId()).isEqualTo(TELEGRAM_ID);
        assertThat(request.totalAmount()).isEqualByComparingTo("6000.00");
        assertThat(request.description()).isEqualTo("iPhone");
        assertThat(request.firstRemainingInstallmentDate()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(request.accountName()).isEqualTo("Nubank");
        assertThat(request.categoryName()).isEqualTo("Eletrônicos");
        assertThat(request.totalInstallments()).isEqualTo(10);
        assertThat(request.firstRemainingInstallmentNumber()).isEqualTo(6);
        assertThat(result).contains("Parcelamento registrado com sucesso");
    }

    @Test
    @DisplayName("deve mapear erro HTTP ao confirmar parcelamento existente")
    void shouldMapHttpErrorWhenConfirmingExistingInstallment() {
        PendingTelegramTransaction pending = existingInstallmentPending();
        when(telegramPendingConfirmationService.getPending(TELEGRAM_ID)).thenReturn(pending);
        doThrow(HttpClientErrorException.create(
                HttpStatus.NOT_FOUND,
                "Not Found",
                HttpHeaders.EMPTY,
                new byte[0],
                null
        )).when(financeBotApiClient).createExistingInstallmentTransaction(any());

        String result = handler.handleConfirmation(TELEGRAM_ID);

        assertThat(result).contains("Não encontrei uma conta vinculada");
        verify(telegramPendingConfirmationService, never()).clearPending(TELEGRAM_ID);
    }

    @Test
    @DisplayName("deve manter pendencia quando erro inesperado acontece ao confirmar parcelamento existente")
    void shouldKeepPendingWhenUnexpectedErrorHappensOnExistingInstallmentConfirmation() {
        PendingTelegramTransaction pending = existingInstallmentPending();
        when(telegramPendingConfirmationService.getPending(TELEGRAM_ID)).thenReturn(pending);
        doThrow(new IllegalStateException("API unavailable"))
                .when(financeBotApiClient)
                .createExistingInstallmentTransaction(any());

        String result = handler.handleConfirmation(TELEGRAM_ID);

        assertThat(result).contains("Não foi possível salvar sua transação agora");
        verify(telegramPendingConfirmationService, never()).clearPending(TELEGRAM_ID);
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

    private PendingTelegramTransaction existingInstallmentPending() {
        return new PendingTelegramTransaction(
                TelegramIntentType.CREATE_EXISTING_INSTALLMENT_EXPENSE,
                new BigDecimal("6000.00"),
                "iPhone",
                LocalDate.of(2026, 6, 15),
                "Eletrônicos",
                "Nubank",
                10,
                6,
                "comprei um iPhone de 6000 em 10x e ja paguei 5 parcelas"
        );
    }
}
