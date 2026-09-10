package com.financebot.infra.messaging;

import com.financebot.reminder.adapter.out.messaging.ReminderNotificationMessage;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitMQConfig {

    @Bean
    TopicExchange notificationExchange() {
        return new TopicExchange(QueueNames.NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    Queue reminderNotificationQueue() {
        return new Queue(QueueNames.REMINDER_NOTIFICATION_QUEUE, true);
    }

    @Bean
    Binding reminderNotificationBinding(Queue reminderNotificationQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(reminderNotificationQueue)
                .to(notificationExchange)
                .with(QueueNames.REMINDER_NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
        typeMapper.setIdClassMapping(Map.of(
                QueueNames.REMINDER_NOTIFICATION_MESSAGE_TYPE,
                ReminderNotificationMessage.class
        ));
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
