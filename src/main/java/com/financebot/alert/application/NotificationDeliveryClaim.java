package com.financebot.alert.application;

public record NotificationDeliveryClaim(String token, Long telegramId, String title, String body) {
}
