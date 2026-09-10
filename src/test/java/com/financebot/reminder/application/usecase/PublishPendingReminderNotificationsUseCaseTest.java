package com.financebot.reminder.application.usecase;

import com.financebot.reminder.application.event.ReminderNotificationEvent;
import com.financebot.reminder.application.port.out.ReminderNotificationPublisherPort;
import com.financebot.reminder.domain.Reminder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishPendingReminderNotificationsUseCaseTest {
    @Mock private ReminderUseCase reminderUseCase;
    @Mock private ReminderNotificationPublisherPort publisherPort;
    private PublishPendingReminderNotificationsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new PublishPendingReminderNotificationsUseCase(reminderUseCase, publisherPort);
    }

    @Test
    void publishesClaimedReminderAsApplicationEvent() {
        LocalDate date = LocalDate.of(2026, 9, 9);
        Reminder reminder = reminder(1L, 123L, "Pagar aluguel", date);
        when(reminderUseCase.claimPending(date)).thenReturn(List.of(reminder));

        ReminderPublicationResult result = useCase.execute(date);

        ArgumentCaptor<ReminderNotificationEvent> event = ArgumentCaptor.forClass(ReminderNotificationEvent.class);
        verify(publisherPort).publish(event.capture());
        assertThat(event.getValue()).isEqualTo(new ReminderNotificationEvent(1L, 123L, "Pagar aluguel", date));
        assertThat(result).isEqualTo(new ReminderPublicationResult(1, 0));
    }

    @Test
    void releasesClaimAndContinuesWhenPublicationFails() {
        LocalDate date = LocalDate.of(2026, 9, 9);
        Reminder first = reminder(1L, 123L, "Aluguel", date);
        Reminder second = reminder(2L, 456L, "Internet", date);
        when(reminderUseCase.claimPending(date)).thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("broker unavailable"))
                .when(publisherPort).publish(any(ReminderNotificationEvent.class));

        ReminderPublicationResult result = useCase.execute(date);

        verify(reminderUseCase).releaseClaim(1L);
        verify(reminderUseCase).releaseClaim(2L);
        assertThat(result).isEqualTo(new ReminderPublicationResult(0, 2));
    }

    private Reminder reminder(Long id, Long telegramId, String description, LocalDate date) {
        Reminder reminder = new Reminder();
        reminder.setId(id);
        reminder.setTelegramId(telegramId);
        reminder.setDescription(description);
        reminder.setReminderDate(date);
        return reminder;
    }
}
