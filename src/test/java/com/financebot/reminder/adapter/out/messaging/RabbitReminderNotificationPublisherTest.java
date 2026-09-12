package com.financebot.reminder.adapter.out.messaging;

import com.financebot.infra.messaging.QueueNames;
import com.financebot.reminder.application.event.ReminderNotificationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitReminderNotificationPublisherTest {
    @Mock private RabbitTemplate rabbitTemplate;

    @Test
    void mapsAndPublishesMessageWithExpectedRoute() {
        RabbitReminderNotificationPublisher publisher = new RabbitReminderNotificationPublisher(rabbitTemplate);

        publisher.publish(new ReminderNotificationEvent(
                1L, 123L, "Pagar aluguel", LocalDate.of(2026, 9, 9)));

        verify(rabbitTemplate).convertAndSend(
                QueueNames.NOTIFICATION_EXCHANGE,
                QueueNames.REMINDER_NOTIFICATION_ROUTING_KEY,
                new ReminderNotificationMessage(1L, 123L, "Pagar aluguel", "2026-09-09")
        );
    }
}
