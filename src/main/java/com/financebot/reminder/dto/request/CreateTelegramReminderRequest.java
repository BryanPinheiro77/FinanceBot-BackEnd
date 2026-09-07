package com.financebot.reminder.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateTelegramReminderRequest(
        @NotNull Long telegramId,
        @NotBlank @Size(max = 255) String description,
        LocalDate reminderDate,
        @Min(0) Integer daysBefore,
        @Size(max = 255) String recurringDescription
) {
}
