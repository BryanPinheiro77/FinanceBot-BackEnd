package com.financebot.telegrambot.alert;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FinancialAlertRabbitConsumer {
    private final DeliverFinancialNotificationUseCase useCase;

    @RabbitListener(queues = FinancialAlertMessagingTopology.FINANCIAL_ALERT_NOTIFICATION_QUEUE)
    public void consume(FinancialAlertNotificationMessage message) {
        if (message == null || message.notificationId() == null
                || !message.notificationId().matches("[0-9a-f]{64}")) {
            return;
        }
        // A API mantém o item persistido e o republica se a reserva não começou.
        useCase.execute(message.notificationId());
    }
}
