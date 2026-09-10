package com.financebot.reminder.adapter.out.messaging;

public record ReminderNotificationMessage(
        Long reminderId,
        Long telegramId,
        String description,
        String reminderDate
) {
}
