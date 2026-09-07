package com.financebot.telegrambot.handler;

import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.reminder.application.port.out.ReminderGateway;
import com.financebot.telegrambot.support.TelegramBotErrorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class TelegramReminderHandler {

    private static final Pattern DAYS_BEFORE = Pattern.compile(
            "\\b(um|uma|dois|duas|tres|quatro|cinco|seis|sete|\\d+)\\s+dias?\\s+antes\\b"
    );
    private static final Map<String, Integer> NUMBER_WORDS = Map.of(
            "um", 1, "uma", 1, "dois", 2, "duas", 2, "tres", 3,
            "quatro", 4, "cinco", 5, "seis", 6, "sete", 7
    );

    private final ReminderGateway reminderGateway;
    private final TelegramBotErrorMapper telegramBotErrorMapper;

    public String handle(Long telegramId, ParsedTelegramMessage message) {
        Integer daysBefore = extractDaysBefore(message.originalMessage());
        boolean linkedToRecurrence = message.date() == null && daysBefore != null;
        if ((!linkedToRecurrence && message.date() == null)
                || message.description() == null || message.description().isBlank()) {
            return "Não consegui identificar a data e a descrição. Exemplo: <code>me lembre dia 10 de pagar o aluguel</code>.";
        }

        try {
            reminderGateway.createReminder(
                    telegramId, message.description(), message.date(), daysBefore == null ? 0 : daysBefore,
                    linkedToRecurrence ? message.description() : null
            );
            return linkedToRecurrence
                    ? "✅ <b>Lembrete criado!</b>\nVou avisar " + daysBefore + " dia(s) antes de " + message.description() + "."
                    : "✅ <b>Lembrete criado!</b>\nVou avisar você em " + message.date() + ".";
        } catch (RestClientResponseException exception) {
            return telegramBotErrorMapper.mapDefaultBotErrors(exception);
        } catch (Exception exception) {
            return "Não foi possível criar o lembrete agora.";
        }
    }

    private Integer extractDaysBefore(String text) {
        if (text == null) return null;
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        Matcher matcher = DAYS_BEFORE.matcher(normalized);
        if (!matcher.find()) return null;
        Integer numberWord = NUMBER_WORDS.get(matcher.group(1));
        return numberWord != null ? numberWord : Integer.valueOf(matcher.group(1));
    }
}
