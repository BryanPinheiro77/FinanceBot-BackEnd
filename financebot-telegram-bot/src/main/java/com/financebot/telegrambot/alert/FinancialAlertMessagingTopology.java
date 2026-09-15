package com.financebot.telegrambot.alert;

final class FinancialAlertMessagingTopology {
    static final String NOTIFICATION_EXCHANGE = "financebot.notifications";
    static final String FINANCIAL_ALERT_NOTIFICATION_QUEUE = "financebot.notifications.financial-alert";
    static final String FINANCIAL_ALERT_NOTIFICATION_ROUTING_KEY = "notification.financial-alert.telegram";
    static final String FINANCIAL_ALERT_NOTIFICATION_MESSAGE_TYPE = "financial-alert-notification-v1";

    private FinancialAlertMessagingTopology() {
    }
}
