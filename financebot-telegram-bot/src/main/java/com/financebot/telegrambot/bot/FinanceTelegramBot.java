package com.financebot.telegrambot.bot;

import com.financebot.telegrambot.config.TelegramBotProperties;
import com.financebot.telegrambot.service.TelegramCommandService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FinanceTelegramBot implements LongPollingUpdateConsumer {

    private final TelegramBotProperties telegramBotProperties;
    private final TelegramCommandService telegramCommandService;

    private TelegramClient telegramClient;
    private TelegramBotsLongPollingApplication botsApplication;

    @PostConstruct
    public void init() {
        try {
            this.telegramClient = new OkHttpTelegramClient(telegramBotProperties.token());
            this.botsApplication = new TelegramBotsLongPollingApplication();

            botsApplication.registerBot(telegramBotProperties.token(), this);

            System.out.println("Bot do Telegram iniciado com sucesso: " + telegramBotProperties.username());
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao inicializar o bot do Telegram", e);
        }
    }

    @Override
    public void consume(List<Update> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }

        for (Update update : updates) {
            if (update == null || !update.hasMessage() || !update.getMessage().hasText()) {
                continue;
            }

            String chatId = update.getMessage().getChatId().toString();
            Long telegramId = update.getMessage().getChatId();
            String telegramUsername = update.getMessage().getFrom() != null
                    ? update.getMessage().getFrom().getUserName()
                    : null;
            String telegramFirstName = update.getMessage().getFrom() != null
                    ? update.getMessage().getFrom().getFirstName()
                    : null;
            String messageText = update.getMessage().getText();

            String responseText = telegramCommandService.handleMessage(
                    messageText,
                    telegramId,
                    telegramUsername,
                    telegramFirstName
            );

            sendMessage(chatId, responseText);
        }
    }

    private void sendMessage(String chatId, String text) {
        try {
            SendMessage sendMessage = SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .build();

            telegramClient.execute(sendMessage);
        } catch (Exception e) {
            System.err.println("Erro ao enviar mensagem no Telegram: " + e.getMessage());
        }
    }
}