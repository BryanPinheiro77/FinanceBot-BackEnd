package com.financebot.reminder.adapter.out.messaging;

import com.financebot.infra.messaging.QueueNames;
import com.financebot.reminder.application.event.ReminderNotificationEvent;
import com.financebot.reminder.application.port.out.ReminderNotificationPublisherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitReminderNotificationPublisher implements ReminderNotificationPublisherPort {
    private final RabbitTemplate rabbitTemplate;

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
                message
        );
    }
}
