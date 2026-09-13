package com.financebot.reminder.adapter.out.messaging;

import com.financebot.infra.messaging.QueueNames;
import com.financebot.reminder.application.event.ReminderNotificationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitReminderNotificationPublisherTest {
    @Mock private RabbitTemplate rabbitTemplate;

    @Test
    void mapsAndPublishesMessageWithExpectedRoute() {
        RabbitReminderNotificationPublisher publisher = new RabbitReminderNotificationPublisher(rabbitTemplate, 3_600_000L);

        publisher.publish(new ReminderNotificationEvent(
                1L, 123L, "Pagar aluguel", LocalDate.of(2026, 9, 9)));

        ArgumentCaptor<MessagePostProcessor> postProcessor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                eq(QueueNames.NOTIFICATION_EXCHANGE),
                eq(QueueNames.REMINDER_NOTIFICATION_ROUTING_KEY),
                eq(new ReminderNotificationMessage(1L, 123L, "Pagar aluguel", "2026-09-09")),
                postProcessor.capture()
        );

        Message rabbitMessage = new Message(new byte[0]);
        postProcessor.getValue().postProcessMessage(rabbitMessage);
        assertThat(rabbitMessage.getMessageProperties().getExpiration()).isEqualTo("3600000");
    }
}
