package com.financebot.alert.adapter;

import com.financebot.alert.application.FinancialAlertNotificationEvent;
import com.financebot.alert.application.FinancialAlertNotificationPublisher;
import com.financebot.infra.messaging.QueueNames;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitFinancialAlertNotificationPublisher implements FinancialAlertNotificationPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final long messageTtlMs;

    public RabbitFinancialAlertNotificationPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${financebot.alerts.queue-message-ttl-ms:604800000}") long messageTtlMs
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.messageTtlMs = messageTtlMs;
    }

    @Override
    public void publish(FinancialAlertNotificationEvent event) {
        rabbitTemplate.convertAndSend(
                QueueNames.NOTIFICATION_EXCHANGE,
                QueueNames.FINANCIAL_ALERT_NOTIFICATION_ROUTING_KEY,
                event,
                message -> {
                    message.getMessageProperties().setExpiration(Long.toString(messageTtlMs));
                    return message;
                }
        );
    }
}
