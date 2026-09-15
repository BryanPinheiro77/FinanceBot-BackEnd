package com.financebot.telegrambot.alert;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
@RequiredArgsConstructor
public class TelegramFinancialNotificationSender implements FinancialNotificationSender {
    private final TelegramClient telegramClient;

    @Override
    public DeliveryOutcome send(NotificationDeliveryClaim claim) {
        try {
            telegramClient.execute(SendMessage.builder().chatId(claim.telegramId())
                    .text("🔔 <b>" + escapeHtml(claim.title()) + "</b>\n" + escapeHtml(claim.body()))
                    .parseMode("HTML").build());
            return DeliveryOutcome.SENT;
        } catch (TelegramApiRequestException exception) {
            // Rejeições 4xx são explícitas; falhas 5xx podem ter resultado ambíguo.
            Integer code = exception.getErrorCode();
            return code != null && code >= 400 && code < 500 ? DeliveryOutcome.REJECTED : DeliveryOutcome.UNKNOWN;
        } catch (Exception exception) {
            return DeliveryOutcome.UNKNOWN;
        }
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
