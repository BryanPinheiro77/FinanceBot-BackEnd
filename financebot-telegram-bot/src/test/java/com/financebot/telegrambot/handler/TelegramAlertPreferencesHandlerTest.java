package com.financebot.telegrambot.handler;

import com.financebot.telegrambot.alert.AlertPreferences;
import com.financebot.telegrambot.client.FinanceBotApiClient;
import com.financebot.telegrambot.service.TelegramPendingConfirmationService;
import com.financebot.telegrambot.service.TelegramPendingQueryService;
import com.financebot.telegrambot.formatter.TelegramAccountMessageFormatter;
import com.financebot.telegrambot.support.TelegramBotErrorMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramAlertPreferencesHandlerTest {
    @Mock FinanceBotApiClient client;
    @Mock TelegramPendingConfirmationService confirmations;
    @Mock TelegramPendingQueryService queries;
    @Mock TelegramAccountMessageFormatter formatter;
    @Mock TelegramBotErrorMapper errors;
    @InjectMocks TelegramBasicCommandHandler handler;

    @Test
    void disableAllUpdatesOnlyAuthenticatedChatPreferences() {
        when(client.getAlertPreferences(123L)).thenReturn(new AlertPreferences(true, true, true));
        when(client.updateAlertPreferences(123L, new AlertPreferences(false, false, false)))
                .thenReturn(new AlertPreferences(false, false, false));
        assertThat(handler.handleAlerts("/alertas desligar", 123L)).contains("Alertas: desligados", "semanal: desligado");
        verify(client).updateAlertPreferences(123L, new AlertPreferences(false, false, false));
    }

    @Test
    void weeklyChangePreservesOtherPreferences() {
        when(client.getAlertPreferences(123L)).thenReturn(new AlertPreferences(true, true, false));
        when(client.updateAlertPreferences(123L, new AlertPreferences(true, false, false)))
                .thenReturn(new AlertPreferences(true, false, false));
        handler.handleAlerts("/alertas semanal desligar", 123L);
        verify(client).updateAlertPreferences(123L, new AlertPreferences(true, false, false));
    }

    @Test
    void queryDoesNotChangePreferences() {
        when(client.getAlertPreferences(123L)).thenReturn(new AlertPreferences(true, false, true));
        assertThat(handler.handleAlerts("/alertas", 123L)).contains("semanal: desligado", "mensal: ligado");
        verify(client, never()).updateAlertPreferences(anyLong(), any());
    }

    @Test
    void invalidCommandDoesNotChangePreferences() {
        assertThat(handler.handleAlerts("/alertas talvez", 123L)).startsWith("Use /alertas");
        verifyNoInteractions(client);
    }

    @Test
    void backendFailureIsExplainedWithoutPretendingPreferencesChanged() {
        when(client.getAlertPreferences(123L)).thenThrow(new IllegalStateException("API unavailable"));
        assertThat(handler.handleAlerts("/alertas desligar", 123L)).contains("Não foi possível");
        verify(client, never()).updateAlertPreferences(anyLong(), any());
    }
}
