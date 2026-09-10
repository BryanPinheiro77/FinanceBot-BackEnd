package com.financebot.infra.messaging;

public final class QueueNames {
    public static final String NOTIFICATION_EXCHANGE = "financebot.notifications";
    public static final String REMINDER_NOTIFICATION_QUEUE = "financebot.notifications.telegram";
    public static final String REMINDER_NOTIFICATION_ROUTING_KEY = "notification.reminder.telegram";
    public static final String REMINDER_NOTIFICATION_MESSAGE_TYPE = "reminder-notification-v1";

    private QueueNames() {
    }
}
