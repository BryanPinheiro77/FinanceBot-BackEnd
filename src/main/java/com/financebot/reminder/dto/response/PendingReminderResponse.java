package com.financebot.reminder.dto.response;

import java.time.LocalDate;

public record PendingReminderResponse(
        Long id,
        Long telegramId,
        String description,
        LocalDate reminderDate
) {
}
