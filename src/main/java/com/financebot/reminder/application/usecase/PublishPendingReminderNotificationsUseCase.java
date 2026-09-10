package com.financebot.reminder.application.usecase;

import com.financebot.reminder.application.event.ReminderNotificationEvent;
import com.financebot.reminder.application.port.out.ReminderNotificationPublisherPort;
import com.financebot.reminder.domain.Reminder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class PublishPendingReminderNotificationsUseCase {
    private final ReminderUseCase reminderUseCase;
    private final ReminderNotificationPublisherPort publisherPort;

    public ReminderPublicationResult execute(LocalDate date) {
        int published = 0;
        int failed = 0;

        for (Reminder reminder : reminderUseCase.claimPending(date)) {
            try {
                publisherPort.publish(toEvent(reminder));
                published++;
            } catch (RuntimeException exception) {
                releaseAfterFailure(reminder.getId());
                failed++;
            }
        }

        return new ReminderPublicationResult(published, failed);
    }

    private void releaseAfterFailure(Long reminderId) {
        try {
            reminderUseCase.releaseClaim(reminderId);
        } catch (RuntimeException ignored) {
            // A reserva expira automaticamente caso a API não consiga liberá-la agora.
        }
    }

    private ReminderNotificationEvent toEvent(Reminder reminder) {
        return new ReminderNotificationEvent(
                reminder.getId(),
                reminder.getTelegramId(),
                reminder.getDescription(),
                reminder.getReminderDate()
        );
    }
}
