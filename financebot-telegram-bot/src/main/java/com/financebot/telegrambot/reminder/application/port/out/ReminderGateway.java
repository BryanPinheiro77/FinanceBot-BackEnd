package com.financebot.telegrambot.reminder.application.port.out;

import com.financebot.telegrambot.dto.response.PendingReminderResponse;

import java.time.LocalDate;
import java.util.List;

public interface ReminderGateway {
    List<PendingReminderResponse> claimPendingReminders();
    void markReminderSent(Long reminderId);
    void releaseReminder(Long reminderId);
    void createReminder(Long telegramId, String description, LocalDate reminderDate,
                        Integer daysBefore, String recurringDescription);
}
