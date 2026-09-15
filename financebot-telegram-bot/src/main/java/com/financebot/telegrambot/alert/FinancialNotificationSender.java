package com.financebot.telegrambot.alert;

public interface FinancialNotificationSender {
    DeliveryOutcome send(NotificationDeliveryClaim claim);
    enum DeliveryOutcome { SENT, REJECTED, UNKNOWN }
}
