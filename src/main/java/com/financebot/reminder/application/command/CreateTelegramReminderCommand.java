package com.financebot.reminder.application.command;

import java.time.LocalDate;

public record CreateTelegramReminderCommand(
        Long telegramId,
        String description,
        LocalDate reminderDate,
        Integer daysBefore,
        String recurringDescription
) {
}
