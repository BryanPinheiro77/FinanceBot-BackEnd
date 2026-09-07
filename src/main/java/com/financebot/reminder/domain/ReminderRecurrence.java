package com.financebot.reminder.domain;

import com.financebot.recurring.domain.RecurrenceFrequency;

import java.time.LocalDate;

public record ReminderRecurrence(Long id, String description, LocalDate nextExecutionDate,
                                 RecurrenceFrequency frequency, boolean active) {
}
