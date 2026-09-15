package com.financebot.telegrambot.alert;

public record NotificationDeliveryClaim(String token, Long telegramId, String title, String body) { }
