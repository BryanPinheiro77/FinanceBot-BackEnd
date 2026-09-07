package com.financebot.telegrambot.reminder.application.port.out;

import com.financebot.telegrambot.dto.response.PendingReminderResponse;

public interface ReminderNotificationPort {
    boolean send(PendingReminderResponse reminder);
}
