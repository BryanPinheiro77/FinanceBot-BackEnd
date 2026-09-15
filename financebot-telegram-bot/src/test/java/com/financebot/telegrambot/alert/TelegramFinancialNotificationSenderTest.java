package com.financebot.telegrambot.alert;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;
import com.financebot.telegrambot.alert.FinancialNotificationSender.DeliveryOutcome;

class TelegramFinancialNotificationSenderTest {
    @Test
    void sendsEscapedFinancialText() throws Exception {
        TelegramClient client = mock(TelegramClient.class);
        new TelegramFinancialNotificationSender(client)
                .send(new NotificationDeliveryClaim("token", 123L, "<Título>", "A & B"));
        var captor = org.mockito.ArgumentCaptor.forClass(SendMessage.class);
        verify(client).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("&lt;Título&gt;", "A &amp; B");
        assertThat(captor.getValue().getChatId()).isEqualTo("123");
    }

    @Test
    void explicitTelegramRejectionCanBeRetried() throws Exception {
        TelegramClient client = mock(TelegramClient.class);
        TelegramApiRequestException rejection = mock(TelegramApiRequestException.class);
        when(rejection.getErrorCode()).thenReturn(429);
        when(client.execute(any(SendMessage.class))).thenThrow(rejection);
        assertThat(new TelegramFinancialNotificationSender(client)
                .send(new NotificationDeliveryClaim("token", 123L, "Título", "Corpo")))
                .isEqualTo(DeliveryOutcome.REJECTED);
    }

    @Test
    void serverErrorIsUncertainRatherThanAutomaticallyRetried() throws Exception {
        TelegramClient client = mock(TelegramClient.class);
        TelegramApiRequestException rejection = mock(TelegramApiRequestException.class);
        when(rejection.getErrorCode()).thenReturn(500);
        when(client.execute(any(SendMessage.class))).thenThrow(rejection);
        assertThat(new TelegramFinancialNotificationSender(client)
                .send(new NotificationDeliveryClaim("token", 123L, "Título", "Corpo")))
                .isEqualTo(DeliveryOutcome.UNKNOWN);
    }

    @Test
    void networkFailureIsUncertain() throws Exception {
        TelegramClient client = mock(TelegramClient.class);
        when(client.execute(any(SendMessage.class))).thenThrow(new RuntimeException("timeout"));
        assertThat(new TelegramFinancialNotificationSender(client)
                .send(new NotificationDeliveryClaim("token", 123L, "Título", "Corpo")))
                .isEqualTo(DeliveryOutcome.UNKNOWN);
    }
}
