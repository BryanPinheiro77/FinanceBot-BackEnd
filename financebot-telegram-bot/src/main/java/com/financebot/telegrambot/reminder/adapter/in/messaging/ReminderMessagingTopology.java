package com.financebot.telegrambot.reminder.adapter.in.messaging;

final class ReminderMessagingTopology {
    static final String NOTIFICATION_EXCHANGE = "financebot.notifications";
    static final String REMINDER_NOTIFICATION_QUEUE = "financebot.notifications.telegram";
    static final String REMINDER_NOTIFICATION_ROUTING_KEY = "notification.reminder.telegram";
    static final String REMINDER_NOTIFICATION_MESSAGE_TYPE = "reminder-notification-v1";

    private ReminderMessagingTopology() {
    }
}
