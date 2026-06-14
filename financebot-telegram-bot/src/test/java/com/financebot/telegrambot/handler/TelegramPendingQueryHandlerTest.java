package com.financebot.telegrambot.handler;

import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.intent.TelegramIntentType;
import com.financebot.telegrambot.service.TelegramIntentService;
import com.financebot.telegrambot.service.TelegramPendingQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramPendingQueryHandlerTest {

    @Mock
    private TelegramIntentService telegramIntentService;

    @Mock
    private TelegramPendingQueryService telegramPendingQueryService;

    @Mock
    private TelegramFinancialQueryHandler telegramFinancialQueryHandler;

    private TelegramPendingQueryHandler telegramPendingQueryHandler;

    @BeforeEach
    void setUp() {
        telegramPendingQueryHandler = new TelegramPendingQueryHandler(
                telegramIntentService,
                telegramPendingQueryService,
                telegramFinancialQueryHandler
        );
    }

    @Test
    @DisplayName("deve identificar query pendente de parcelamento")
    void shouldIdentifyPendingInstallmentQuery() {
        when(telegramPendingQueryService.getPending(123L)).thenReturn(pendingQuery(null));

        boolean result = telegramPendingQueryHandler.hasPendingInstallmentQuery(123L, "notebook");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("nao deve continuar query pendente quando mensagem e comando")
    void shouldNotContinuePendingQueryWhenMessageIsCommand() {
        when(telegramPendingQueryService.getPending(123L)).thenReturn(pendingQuery(null));

        boolean result = telegramPendingQueryHandler.hasPendingInstallmentQuery(123L, "/help");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("deve aplicar alvo escolhido e delegar para consulta financeira")
    void shouldApplySelectedTargetAndDelegateToFinancialQueryHandler() {
        ParsedTelegramMessage pending = pendingQuery(null);
        ParsedTelegramMessage reparsed = pendingQuery("notebook");

        when(telegramPendingQueryService.getPending(123L)).thenReturn(pending);
        when(telegramIntentService.parse("notebook")).thenReturn(reparsed);
        when(telegramFinancialQueryHandler.handleQuery(org.mockito.Mockito.any(), org.mockito.Mockito.eq(123L)))
                .thenReturn("faltam 5 parcelas");

        String result = telegramPendingQueryHandler.handlePendingInstallmentQuerySelection(123L, "notebook");

        ArgumentCaptor<ParsedTelegramMessage> parsedCaptor = ArgumentCaptor.forClass(ParsedTelegramMessage.class);
        verify(telegramPendingQueryService).clearPending(123L);
        verify(telegramFinancialQueryHandler).handleQuery(parsedCaptor.capture(), org.mockito.Mockito.eq(123L));

        assertThat(result).isEqualTo("faltam 5 parcelas");
        assertThat(parsedCaptor.getValue().installmentQueryTarget()).isEqualTo("notebook");
        assertThat(parsedCaptor.getValue().intentType()).isEqualTo(TelegramIntentType.QUERY_INSTALLMENT_REMAINING);
    }

    @Test
    @DisplayName("deve retornar mensagem quando nao ha consulta pendente")
    void shouldReturnMessageWhenThereIsNoPendingQuery() {
        String result = telegramPendingQueryHandler.handlePendingInstallmentQuerySelection(123L, "notebook");

        assertThat(result).isEqualTo("Não há nenhuma consulta pendente para continuar.");
    }

    private ParsedTelegramMessage pendingQuery(String target) {
        return new ParsedTelegramMessage(
                TelegramIntentType.QUERY_INSTALLMENT_REMAINING,
                null,
                null,
                null,
                "quantas parcelas faltam?",
                null,
                null,
                null,
                null,
                null,
                null,
                target,
                null,
                null
        );
    }
}
