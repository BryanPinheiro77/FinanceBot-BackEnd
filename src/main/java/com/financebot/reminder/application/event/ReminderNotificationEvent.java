package com.financebot.reminder.application.event;

import java.time.LocalDate;

public record ReminderNotificationEvent(
        Long reminderId,
        Long telegramId,
        String description,
        LocalDate reminderDate
) {
}
