package com.financebot.telegrambot.alert;

public interface FinancialNotificationGateway {
    NotificationDeliveryClaim claim(String id);
    void complete(String id, String token, String outcome);
}
