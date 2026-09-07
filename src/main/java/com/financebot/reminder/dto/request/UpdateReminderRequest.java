package com.financebot.reminder.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateReminderRequest(
        @NotBlank @Size(max = 255) String description,
        @NotNull LocalDate reminderDate,
        @NotNull @Min(0) Integer daysBefore,
        @NotNull Boolean active
) {
}
