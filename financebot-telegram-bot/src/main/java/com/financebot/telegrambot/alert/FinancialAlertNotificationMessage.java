package com.financebot.telegrambot.alert;

public record FinancialAlertNotificationMessage(
        String notificationId,
        Long telegramId,
        String title,
        String body,
        String deduplicationKey
) {
}
