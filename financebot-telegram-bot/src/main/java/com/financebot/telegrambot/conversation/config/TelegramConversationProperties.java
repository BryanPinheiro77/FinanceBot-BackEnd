package com.financebot.telegrambot.conversation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "telegram.conversation")
public record TelegramConversationProperties(
        Duration contextTtl
) {
}
