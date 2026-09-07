package com.financebot.telegrambot.reminder.adapter.out.telegram;

import com.financebot.telegrambot.dto.response.PendingReminderResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TelegramReminderNotificationAdapterTest {

    @Test
    void sendsEscapedReminderMessageToTelegram() throws Exception {
        TelegramClient telegramClient = mock(TelegramClient.class);
        TelegramReminderNotificationAdapter adapter = new TelegramReminderNotificationAdapter(telegramClient);
        PendingReminderResponse reminder = new PendingReminderResponse(
                1L, 123L, "Pagar internet <hoje> & guardar comprovante", LocalDate.now());

        boolean sent = adapter.send(reminder);

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient).execute(messageCaptor.capture());
        SendMessage message = messageCaptor.getValue();
        assertThat(sent).isTrue();
        assertThat(message.getChatId()).isEqualTo("123");
        assertThat(message.getParseMode()).isEqualTo("HTML");
        assertThat(message.getText()).isEqualTo(
                "🔔 <b>Lembrete financeiro</b>\nPagar internet &lt;hoje&gt; &amp; guardar comprovante");
    }
}
