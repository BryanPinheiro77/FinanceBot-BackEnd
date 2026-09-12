package com.financebot.telegrambot.reminder.application;

import com.financebot.telegrambot.dto.response.PendingReminderResponse;
import com.financebot.telegrambot.reminder.application.port.out.ReminderGateway;
import com.financebot.telegrambot.reminder.application.port.out.ReminderNotificationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import static com.financebot.telegrambot.reminder.application.ReminderDeliveryResult.DELIVERED;
import static com.financebot.telegrambot.reminder.application.ReminderDeliveryResult.RELEASED;
import static com.financebot.telegrambot.reminder.application.ReminderDeliveryResult.SKIPPED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendPendingRemindersUseCaseTest {
    @Mock private ReminderGateway reminderGateway;
    @Mock private ReminderNotificationPort notificationPort;
    private SendPendingRemindersUseCase useCase;
    private PendingReminderResponse reminder;

    @BeforeEach
    void setUp() {
        useCase = new SendPendingRemindersUseCase(reminderGateway, notificationPort);
        reminder = new PendingReminderResponse(1L, 123L, "Pagar aluguel", LocalDate.now());
    }

    @Test
    void marksReminderAfterSuccessfulDelivery() {
        when(reminderGateway.isDeliverable(1L, reminder.reminderDate())).thenReturn(true);
        when(notificationPort.send(reminder)).thenReturn(true);

        ReminderDeliveryResult result = useCase.execute(reminder);

        verify(reminderGateway).markReminderSent(1L);
        assertThat(result).isEqualTo(DELIVERED);
    }

    @Test
    void releasesClaimWhenDeliveryFails() {
        when(reminderGateway.isDeliverable(1L, reminder.reminderDate())).thenReturn(true);
        when(notificationPort.send(reminder)).thenReturn(false);

        ReminderDeliveryResult result = useCase.execute(reminder);

        verify(reminderGateway).releaseReminder(1L);
        assertThat(result).isEqualTo(RELEASED);
    }

    @Test
    void skipsStaleQueuedDeliveryBeforeSendingToTelegram() {
        when(reminderGateway.isDeliverable(1L, reminder.reminderDate())).thenReturn(false);

        ReminderDeliveryResult result = useCase.execute(reminder);

        assertThat(result).isEqualTo(SKIPPED);
        verify(notificationPort, never()).send(reminder);
        verify(reminderGateway, never()).releaseReminder(1L);
    }
}
