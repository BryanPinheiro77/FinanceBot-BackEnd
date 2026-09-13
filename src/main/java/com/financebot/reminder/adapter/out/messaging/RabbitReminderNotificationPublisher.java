package com.financebot.reminder.adapter.out.messaging;

import com.financebot.infra.messaging.QueueNames;
import com.financebot.reminder.application.event.ReminderNotificationEvent;
import com.financebot.reminder.application.port.out.ReminderNotificationPublisherPort;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitReminderNotificationPublisher implements ReminderNotificationPublisherPort {
    private final RabbitTemplate rabbitTemplate;
    private final long messageTtlMs;

    public RabbitReminderNotificationPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${financebot.reminders.queue-message-ttl-ms:86400000}") long messageTtlMs
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.messageTtlMs = messageTtlMs;
    }

    @Override
    public void publish(ReminderNotificationEvent event) {
        ReminderNotificationMessage message = new ReminderNotificationMessage(
                event.reminderId(),
                event.telegramId(),
                event.description(),
                event.reminderDate().toString()
        );
        rabbitTemplate.convertAndSend(
                QueueNames.NOTIFICATION_EXCHANGE,
                QueueNames.REMINDER_NOTIFICATION_ROUTING_KEY,
                message,
                rabbitMessage -> {
                    rabbitMessage.getMessageProperties().setExpiration(Long.toString(messageTtlMs));
                    return rabbitMessage;
                }
        );
    }
}
