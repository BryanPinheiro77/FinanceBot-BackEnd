package com.financebot.telegrambot.reminder.adapter.out.telegram;

import com.financebot.telegrambot.dto.response.PendingReminderResponse;
import com.financebot.telegrambot.reminder.application.port.out.ReminderNotificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
@RequiredArgsConstructor
public class TelegramReminderNotificationAdapter implements ReminderNotificationPort {
    private final TelegramClient telegramClient;

    @Override
    public boolean send(PendingReminderResponse reminder) {
        try {
            telegramClient.execute(SendMessage.builder()
                    .chatId(reminder.telegramId())
                    .text("🔔 <b>Lembrete financeiro</b>\n" + escapeHtml(reminder.description()))
                    .parseMode("HTML")
                    .build());
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private String escapeHtml(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
