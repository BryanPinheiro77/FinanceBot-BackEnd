package com.financebot.reminder.application.command;

import java.time.LocalDate;

public record CreateReminderCommand(
        String description,
        LocalDate reminderDate,
        Integer daysBefore,
        Long recurringTransactionId,
        Long userId
) {
}
