package com.financebot.alert.adapter;

import com.financebot.alert.application.FinancialAlertNotificationEvent;
import com.financebot.infra.messaging.QueueNames;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class RabbitFinancialAlertNotificationPublisherTest {
    @Test
    void sendsOpaqueIdWithBoundedMessageTtl() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        doAnswer(invocation -> {
            MessagePostProcessor processor = invocation.getArgument(3);
            Message message = processor.postProcessMessage(new Message(new byte[0], new MessageProperties()));
            assertThat(message.getMessageProperties().getExpiration()).isEqualTo("604800000");
            return null;
        }).when(rabbit).convertAndSend(eq(QueueNames.NOTIFICATION_EXCHANGE),
                eq(QueueNames.FINANCIAL_ALERT_NOTIFICATION_ROUTING_KEY), any(Object.class), any(MessagePostProcessor.class));
        new RabbitFinancialAlertNotificationPublisher(rabbit, 604800000)
                .publish(new FinancialAlertNotificationEvent("opaque-id"));
        verify(rabbit).convertAndSend(eq(QueueNames.NOTIFICATION_EXCHANGE),
                eq(QueueNames.FINANCIAL_ALERT_NOTIFICATION_ROUTING_KEY),
                eq(new FinancialAlertNotificationEvent("opaque-id")), any(MessagePostProcessor.class));
    }
}
