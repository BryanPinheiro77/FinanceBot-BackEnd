package com.financebot.telegrambot.handler;

import com.financebot.telegrambot.client.FinanceBotApiClient;
import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.intent.TelegramIntentType;
import com.financebot.telegrambot.support.TelegramBotErrorMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TelegramReminderHandlerTest {

    @Mock
    private FinanceBotApiClient financeBotApiClient;

    @Mock
    private TelegramBotErrorMapper telegramBotErrorMapper;

    @Test
    void createsReminderFromParsedMessage() {
        LocalDate date = LocalDate.of(2026, 10, 10);
        ParsedTelegramMessage message = new ParsedTelegramMessage(
                TelegramIntentType.CREATE_REMINDER, null, "pagar o aluguel", date,
                "me lembre dia 10 de pagar o aluguel", null, null, null, null,
                null, null, null, null, null
        );
        TelegramReminderHandler handler = new TelegramReminderHandler(financeBotApiClient, telegramBotErrorMapper);

        String response = handler.handle(123L, message);

        assertThat(response).contains("Lembrete criado").contains("2026-10-10");
        verify(financeBotApiClient).createReminder(123L, "pagar o aluguel", date, 0);
    }

    @Test
    void asksForMissingReminderData() {
        ParsedTelegramMessage message = new ParsedTelegramMessage(
                TelegramIntentType.CREATE_REMINDER, null, null, null,
                "me lembre", null, null, null, null, null, null, null, null, null
        );
        TelegramReminderHandler handler = new TelegramReminderHandler(financeBotApiClient, telegramBotErrorMapper);

        assertThat(handler.handle(123L, message)).contains("data e a descrição");
    }
}
