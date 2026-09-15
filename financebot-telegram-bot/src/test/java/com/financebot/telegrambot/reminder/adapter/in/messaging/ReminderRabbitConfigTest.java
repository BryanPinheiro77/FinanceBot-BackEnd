package com.financebot.telegrambot.reminder.adapter.in.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ReminderRabbitConfigTest {

    @Test
    void readsFinancialNotificationContractContainingOnlyOpaqueId() {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setHeader("__TypeId__", "financial-alert-notification-v1");
        Message message = new Message("{\"notificationId\":\"opaque-id\"}".getBytes(StandardCharsets.UTF_8), properties);
        Object result = new ReminderRabbitConfig().rabbitMessageConverter().fromMessage(message);
        assertThat(result).isEqualTo(new com.financebot.telegrambot.alert.FinancialAlertNotificationMessage("opaque-id"));
    }

    @Test
    void readsVersionedReminderNotificationContract() {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setHeader("__TypeId__", ReminderMessagingTopology.REMINDER_NOTIFICATION_MESSAGE_TYPE);
        Message message = new Message("""
                {"reminderId":1,"telegramId":123,"description":"Aluguel","reminderDate":"2026-09-09"}
                """.getBytes(StandardCharsets.UTF_8), properties);
        MessageConverter converter = new ReminderRabbitConfig().rabbitMessageConverter();

        Object result = converter.fromMessage(message);

        assertThat(result).isEqualTo(new ReminderNotificationMessage(
                1L, 123L, "Aluguel", "2026-09-09"));
    }
}
