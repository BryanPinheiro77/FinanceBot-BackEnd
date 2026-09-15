package com.financebot.alert.application;

public record FinancialAlertNotificationEvent(
        String notificationId,
        Long telegramId,
        String title,
        String body,
        String deduplicationKey
) {
}
