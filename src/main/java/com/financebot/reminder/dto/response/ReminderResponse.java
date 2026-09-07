package com.financebot.reminder.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReminderResponse(
        Long id,
        String description,
        LocalDate reminderDate,
        int daysBefore,
        boolean active,
        LocalDateTime sentAt,
        Long recurringTransactionId
) {
}
