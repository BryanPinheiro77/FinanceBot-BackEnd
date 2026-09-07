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
import java.util.List;

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
        when(reminderGateway.claimPendingReminders()).thenReturn(List.of(reminder));
    }

    @Test
    void marksReminderAfterSuccessfulDelivery() {
        when(notificationPort.send(reminder)).thenReturn(true);

        useCase.execute();

        verify(reminderGateway).markReminderSent(1L);
    }

    @Test
    void releasesClaimWhenDeliveryFails() {
        when(notificationPort.send(reminder)).thenReturn(false);

        useCase.execute();

        verify(reminderGateway).releaseReminder(1L);
    }
}
