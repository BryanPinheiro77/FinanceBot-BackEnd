package com.financebot.telegrambot.reminder.adapter.in.messaging;

import com.financebot.telegrambot.dto.response.PendingReminderResponse;
import com.financebot.telegrambot.reminder.application.SendPendingRemindersUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static com.financebot.telegrambot.reminder.application.ReminderDeliveryResult.DELIVERED;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RabbitReminderNotificationConsumerTest {
    @Mock private SendPendingRemindersUseCase useCase;

    @Test
    void mapsMessageAndDelegatesDelivery() {
        RabbitReminderNotificationConsumer consumer = new RabbitReminderNotificationConsumer(useCase);
        PendingReminderResponse reminder = new PendingReminderResponse(
                1L, 123L, "Pagar aluguel", LocalDate.of(2026, 9, 9));
        when(useCase.execute(reminder)).thenReturn(DELIVERED);

        consumer.consume(new ReminderNotificationMessage(
                1L, 123L, "Pagar aluguel", "2026-09-09"));

        verify(useCase).execute(reminder);
    }

    @Test
    void discardsMessageWithInvalidDate() {
        RabbitReminderNotificationConsumer consumer = new RabbitReminderNotificationConsumer(useCase);

        consumer.consume(new ReminderNotificationMessage(1L, 123L, "Pagar aluguel", "invalid"));

        verify(useCase, never()).execute(org.mockito.ArgumentMatchers.any());
    }
}
