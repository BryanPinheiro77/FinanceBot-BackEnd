package com.financebot.telegrambot.handler;

import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.intent.TelegramIntentType;
import com.financebot.telegrambot.reminder.application.port.out.ReminderGateway;
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
    private ReminderGateway reminderGateway;

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
        TelegramReminderHandler handler = new TelegramReminderHandler(reminderGateway, telegramBotErrorMapper);

        String response = handler.handle(123L, message);

        assertThat(response).contains("Lembrete criado").contains("2026-10-10");
        verify(reminderGateway).createReminder(123L, "pagar o aluguel", date, 0, null);
    }

    @Test
    void asksForMissingReminderData() {
        ParsedTelegramMessage message = new ParsedTelegramMessage(
                TelegramIntentType.CREATE_REMINDER, null, null, null,
                "me lembre", null, null, null, null, null, null, null, null, null
        );
        TelegramReminderHandler handler = new TelegramReminderHandler(reminderGateway, telegramBotErrorMapper);

        assertThat(handler.handle(123L, message)).contains("data e a descrição");
    }

    @Test
    void createsReminderLinkedToRecurringTransaction() {
        ParsedTelegramMessage message = new ParsedTelegramMessage(
                TelegramIntentType.CREATE_REMINDER, null, "internet", null,
                "me avise dois dias antes da internet vencer", null, null, null, null,
                null, null, null, null, null
        );
        TelegramReminderHandler handler = new TelegramReminderHandler(reminderGateway, telegramBotErrorMapper);

        assertThat(handler.handle(123L, message)).contains("2 dia(s) antes");
        verify(reminderGateway).createReminder(123L, "internet", null, 2, "internet");
    }
}
