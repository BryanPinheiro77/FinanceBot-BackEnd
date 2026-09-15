package com.financebot.telegrambot.alert;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class FinancialAlertRabbitConsumer {

    private static final Duration DEDUPLICATION_TTL = Duration.ofDays(31);

    private final StringRedisTemplate redisTemplate;
    private final TelegramClient telegramClient;

    @RabbitListener(queues = FinancialAlertMessagingTopology.FINANCIAL_ALERT_NOTIFICATION_QUEUE)
    public void consume(FinancialAlertNotificationMessage message) {
        validate(message);
        String key = "financebot:financial-alert:delivered:" + message.notificationId();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", DEDUPLICATION_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            return;
        }
        try {
            telegramClient.execute(SendMessage.builder()
                    .chatId(message.telegramId())
                    .text("⚠️ <b>" + escapeHtml(message.title()) + "</b>\n" + escapeHtml(message.body()))
                    .parseMode("HTML")
                    .build());
        } catch (Exception exception) {
            redisTemplate.delete(key);
            throw new IllegalStateException("Could not deliver financial alert", exception);
        }
    }

    private void validate(FinancialAlertNotificationMessage message) {
        if (message == null || message.notificationId() == null || message.telegramId() == null
                || message.title() == null || message.title().isBlank()
                || message.body() == null || message.body().isBlank()) {
            throw new IllegalArgumentException("Invalid financial alert notification");
        }
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
