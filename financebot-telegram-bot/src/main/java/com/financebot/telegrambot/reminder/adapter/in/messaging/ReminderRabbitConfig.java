package com.financebot.telegrambot.reminder.adapter.in.messaging;

import com.financebot.telegrambot.alert.FinancialAlertNotificationMessage;
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
class ReminderRabbitConfig {

    @Bean
    TopicExchange reminderNotificationExchange() {
        return new TopicExchange(ReminderMessagingTopology.NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    Queue reminderNotificationQueue() {
        return new Queue(ReminderMessagingTopology.REMINDER_NOTIFICATION_QUEUE, true);
    }

    @Bean
    Binding reminderNotificationBinding(Queue reminderNotificationQueue,
                                        TopicExchange reminderNotificationExchange) {
        return BindingBuilder.bind(reminderNotificationQueue)
                .to(reminderNotificationExchange)
                .with(ReminderMessagingTopology.REMINDER_NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    Queue financialAlertNotificationQueue() {
        return new Queue("financebot.notifications.financial-alert", true);
    }

    @Bean
    Binding financialAlertNotificationBinding(
            Queue financialAlertNotificationQueue,
            TopicExchange reminderNotificationExchange
    ) {
        return BindingBuilder.bind(financialAlertNotificationQueue)
                .to(reminderNotificationExchange)
                .with("notification.financial-alert.telegram");
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
        typeMapper.setIdClassMapping(Map.of(
                ReminderMessagingTopology.REMINDER_NOTIFICATION_MESSAGE_TYPE,
                ReminderNotificationMessage.class,
                "financial-alert-notification-v1",
                FinancialAlertNotificationMessage.class
        ));
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
