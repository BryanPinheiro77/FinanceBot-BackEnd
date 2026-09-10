package com.financebot.reminder.application.port.out;

import com.financebot.reminder.application.event.ReminderNotificationEvent;

public interface ReminderNotificationPublisherPort {
    void publish(ReminderNotificationEvent event);
}
