package com.financebot.telegrambot.reminder.application;

import com.financebot.telegrambot.dto.response.PendingReminderResponse;
import com.financebot.telegrambot.reminder.application.port.out.ReminderGateway;
import com.financebot.telegrambot.reminder.application.port.out.ReminderNotificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SendPendingRemindersUseCase {
    private final ReminderGateway reminderGateway;
    private final ReminderNotificationPort notificationPort;

    public void execute() {
        for (PendingReminderResponse reminder : reminderGateway.claimPendingReminders()) {
            try {
                if (notificationPort.send(reminder)) reminderGateway.markReminderSent(reminder.id());
                else reminderGateway.releaseReminder(reminder.id());
            } catch (Exception exception) {
                releaseAfterFailure(reminder.id());
            }
        }
    }

    private void releaseAfterFailure(Long id) {
        try {
            reminderGateway.releaseReminder(id);
        } catch (Exception ignored) {
            // A reserva expira automaticamente e poderá ser retomada em outra execução.
        }
    }
}
