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

    public ReminderDeliveryResult execute(PendingReminderResponse reminder) {
        try {
            if (!reminderGateway.isDeliverable(reminder.id(), reminder.reminderDate())) {
                return ReminderDeliveryResult.SKIPPED;
            }
            if (notificationPort.send(reminder)) {
                reminderGateway.markReminderSent(reminder.id());
                return ReminderDeliveryResult.DELIVERED;
            }
        } catch (Exception exception) {
            releaseAfterFailure(reminder.id());
            return ReminderDeliveryResult.RELEASED;
        }
        releaseAfterFailure(reminder.id());
        return ReminderDeliveryResult.RELEASED;
    }

    private void releaseAfterFailure(Long id) {
        try {
            reminderGateway.releaseReminder(id);
        } catch (Exception ignored) {
            // A reserva expira automaticamente e poderá ser retomada em outra execução.
        }
    }
}
