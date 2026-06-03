package com.financebot.telegrambot.router;

import com.financebot.telegrambot.conversation.application.TelegramConversationContinuationService;
import com.financebot.telegrambot.handler.TelegramBasicCommandHandler;
import com.financebot.telegrambot.handler.TelegramNaturalLanguageHandler;
import com.financebot.telegrambot.handler.TelegramPendingEditHandler;
import com.financebot.telegrambot.handler.TelegramPendingOperationHandler;
import com.financebot.telegrambot.handler.TelegramPendingQueryHandler;
import com.financebot.telegrambot.service.TelegramPendingConfirmationService;
import com.financebot.telegrambot.support.TelegramCommandMatcher;
import com.financebot.telegrambot.support.TelegramTextNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramCommandRouterTest {

    @Mock
    private TelegramPendingConfirmationService telegramPendingConfirmationService;

    @Mock
    private TelegramBasicCommandHandler telegramBasicCommandHandler;

    @Mock
    private TelegramPendingOperationHandler telegramPendingOperationHandler;

    @Mock
    private TelegramPendingEditHandler telegramPendingEditHandler;

    @Mock
    private TelegramPendingQueryHandler telegramPendingQueryHandler;

    @Mock
    private TelegramNaturalLanguageHandler telegramNaturalLanguageHandler;

    @Mock
    private TelegramConversationContinuationService telegramConversationContinuationService;

    private TelegramCommandRouter telegramCommandRouter;

    @BeforeEach
    void setUp() {
        telegramCommandRouter = new TelegramCommandRouter(
                telegramPendingConfirmationService,
                new TelegramCommandMatcher(new TelegramTextNormalizer()),
                telegramBasicCommandHandler,
                telegramPendingOperationHandler,
                telegramPendingEditHandler,
                telegramPendingQueryHandler,
                telegramNaturalLanguageHandler,
                telegramConversationContinuationService
        );
    }

    @Test
    @DisplayName("deve rotear comando de ajuda para handler de comandos basicos")
    void shouldRouteHelpCommandToBasicCommandHandler() {
        when(telegramBasicCommandHandler.handleHelp()).thenReturn("help");

        String result = telegramCommandRouter.route("/help", 123L, "bryan", "Bryan");

        assertThat(result).isEqualTo("help");
        verify(telegramBasicCommandHandler).handleHelp();
    }

    @Test
    @DisplayName("deve rotear confirmacao para handler de operacao pendente")
    void shouldRouteConfirmationToPendingOperationHandler() {
        when(telegramPendingOperationHandler.handleConfirmation(123L)).thenReturn("confirmed");

        String result = telegramCommandRouter.route("confirmar", 123L, "bryan", "Bryan");

        assertThat(result).isEqualTo("confirmed");
        verify(telegramPendingOperationHandler).handleConfirmation(123L);
    }

    @Test
    @DisplayName("deve rotear cancelamento para handler de operacao pendente")
    void shouldRouteCancellationToPendingOperationHandler() {
        when(telegramPendingOperationHandler.handleCancellation(123L)).thenReturn("cancelled");

        String result = telegramCommandRouter.route("cancelar", 123L, "bryan", "Bryan");

        assertThat(result).isEqualTo("cancelled");
        verify(telegramPendingOperationHandler).handleCancellation(123L);
    }

    @Test
    @DisplayName("deve rotear edicao quando existe operacao pendente")
    void shouldRoutePendingEditWhenThereIsPendingConfirmation() {
        when(telegramPendingConfirmationService.hasPending(123L)).thenReturn(true);
        when(telegramPendingEditHandler.handleEdit(123L, "muda valor para 80")).thenReturn("updated");

        String result = telegramCommandRouter.route("muda valor para 80", 123L, "bryan", "Bryan");

        assertThat(result).isEqualTo("updated");
        verify(telegramPendingEditHandler).handleEdit(123L, "muda valor para 80");
    }

    @Test
    @DisplayName("deve rotear continuacao de query pendente de parcelamento")
    void shouldRoutePendingInstallmentQuerySelection() {
        when(telegramPendingQueryHandler.hasPendingInstallmentQuery(123L, "notebook")).thenReturn(true);
        when(telegramPendingQueryHandler.handlePendingInstallmentQuerySelection(123L, "notebook"))
                .thenReturn("query result");

        String result = telegramCommandRouter.route("notebook", 123L, "bryan", "Bryan");

        assertThat(result).isEqualTo("query result");
        verify(telegramPendingQueryHandler).handlePendingInstallmentQuerySelection(123L, "notebook");
    }

    @Test
    @DisplayName("deve rotear continuacao quando existe contexto conversacional pendente")
    void shouldRouteConversationContinuationWhenThereIsPendingContext() {
        when(telegramConversationContinuationService.hasPendingContext(123L)).thenReturn(true);
        when(telegramConversationContinuationService.handle(123L, "dia 15")).thenReturn("preview");

        String result = telegramCommandRouter.route("dia 15", 123L, "bryan", "Bryan");

        assertThat(result).isEqualTo("preview");
        verify(telegramConversationContinuationService).handle(123L, "dia 15");
    }

    @Test
    @DisplayName("deve usar linguagem natural como fallback")
    void shouldRouteFallbackToNaturalLanguageHandler() {
        when(telegramConversationContinuationService.hasPendingContext(123L)).thenReturn(false);
        when(telegramNaturalLanguageHandler.handle("gastei 50 no mercado", 123L)).thenReturn("preview");

        String result = telegramCommandRouter.route("gastei 50 no mercado", 123L, "bryan", "Bryan");

        assertThat(result).isEqualTo("preview");
        verify(telegramNaturalLanguageHandler).handle("gastei 50 no mercado", 123L);
    }
}
