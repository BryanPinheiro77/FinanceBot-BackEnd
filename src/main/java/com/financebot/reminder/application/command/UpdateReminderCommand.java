package com.financebot.reminder.application.command;

import java.time.LocalDate;

public record UpdateReminderCommand(
        String description,
        LocalDate reminderDate,
        int daysBefore,
        boolean active,
        Long userId
) {
}
