package com.financebot.telegrambot.alert;

import org.springframework.stereotype.Component;
import com.financebot.telegrambot.alert.FinancialNotificationSender.DeliveryOutcome;

@Component
public class DeliverFinancialNotificationUseCase {
    private final FinancialNotificationGateway gateway;
    private final FinancialNotificationSender sender;

    public DeliverFinancialNotificationUseCase(FinancialNotificationGateway gateway, FinancialNotificationSender sender) {
        this.gateway = gateway;
        this.sender = sender;
    }

    public void execute(String id) {
        NotificationDeliveryClaim claim = gateway.claim(id);
        if (claim == null) {
            return;
        }
        DeliveryOutcome outcome;
        try {
            outcome = sender.send(claim);
        } catch (RuntimeException exception) {
            outcome = DeliveryOutcome.UNKNOWN;
        }
        String state = outcome == DeliveryOutcome.REJECTED ? "PENDING"
                : outcome == DeliveryOutcome.SENT ? "SENT" : "UNKNOWN";
        RuntimeException failure = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                gateway.complete(id, claim.token(), state);
                return;
            } catch (RuntimeException exception) {
                failure = exception;
            }
        }
        // Nunca reenviar ao Telegram só porque o ACK da API falhou.
        throw new IllegalStateException("Could not acknowledge financial notification", failure);
    }
}
