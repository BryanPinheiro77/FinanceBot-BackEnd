package com.financebot.telegrambot.handler;

import com.financebot.telegrambot.client.FinanceBotApiClient;
import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.support.TelegramBotErrorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class TelegramReminderHandler {

    private final FinanceBotApiClient financeBotApiClient;
    private final TelegramBotErrorMapper telegramBotErrorMapper;

    public String handle(Long telegramId, ParsedTelegramMessage message) {
        if (message.date() == null || message.description() == null || message.description().isBlank()) {
            return "Não consegui identificar a data e a descrição. Exemplo: <code>me lembre dia 10 de pagar o aluguel</code>.";
        }

        try {
            financeBotApiClient.createReminder(telegramId, message.description(), message.date(), 0);
            return "✅ <b>Lembrete criado!</b>\nVou avisar você em " + message.date() + ".";
        } catch (RestClientResponseException exception) {
            return telegramBotErrorMapper.mapDefaultBotErrors(exception);
        } catch (Exception exception) {
            return "Não foi possível criar o lembrete agora.";
        }
    }
}
