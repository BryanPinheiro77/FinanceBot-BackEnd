package com.financebot.infra.messaging;

import com.financebot.reminder.adapter.out.messaging.ReminderNotificationMessage;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQConfigTest {

    @Test
    void declaresDurableReminderTopology() {
        RabbitMQConfig config = new RabbitMQConfig();
        var queue = config.reminderNotificationQueue();
        var exchange = config.notificationExchange();

        assertThat(exchange.isDurable()).isTrue();
        assertThat(queue.isDurable()).isTrue();
        assertThat(config.reminderNotificationBinding(queue, exchange).getRoutingKey())
                .isEqualTo(QueueNames.REMINDER_NOTIFICATION_ROUTING_KEY);
    }

    @Test
    void writesStableMessageTypeInsteadOfJavaClassName() {
        MessageConverter converter = new RabbitMQConfig().rabbitMessageConverter();

        Message message = converter.toMessage(
                new ReminderNotificationMessage(1L, 123L, "Aluguel", "2026-09-09"),
                new MessageProperties()
        );

        assertThat((Object) message.getMessageProperties().getHeader("__TypeId__"))
                .isEqualTo(QueueNames.REMINDER_NOTIFICATION_MESSAGE_TYPE);
    }
}
