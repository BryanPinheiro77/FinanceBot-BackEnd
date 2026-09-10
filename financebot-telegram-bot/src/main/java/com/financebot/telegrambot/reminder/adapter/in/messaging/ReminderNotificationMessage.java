package com.financebot.telegrambot.reminder.adapter.in.messaging;

public record ReminderNotificationMessage(
        Long reminderId,
        Long telegramId,
        String description,
        String reminderDate
) {
}
