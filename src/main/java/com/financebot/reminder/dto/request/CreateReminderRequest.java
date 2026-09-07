package com.financebot.reminder.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateReminderRequest(
        @NotBlank @Size(max = 255) String description,
        LocalDate reminderDate,
        @Min(0) Integer daysBefore,
        Long recurringTransactionId
) {
}
