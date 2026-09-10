package com.financebot.telegrambot.reminder.application.port.out;

import java.time.LocalDate;

public interface ReminderGateway {
    boolean isDeliverable(Long reminderId, LocalDate reminderDate);
    void markReminderSent(Long reminderId);
    void releaseReminder(Long reminderId);
    void createReminder(Long telegramId, String description, LocalDate reminderDate,
                        Integer daysBefore, String recurringDescription);
}
