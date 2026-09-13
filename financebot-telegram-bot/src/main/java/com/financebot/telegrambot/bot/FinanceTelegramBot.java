package com.financebot.telegrambot.bot;

import com.financebot.telegrambot.config.TelegramBotProperties;
import com.financebot.telegrambot.media.application.TelegramMediaMessageHandler;
import com.financebot.telegrambot.observability.FinanceBotMetrics;
import com.financebot.telegrambot.service.TelegramCommandService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static com.financebot.telegrambot.observability.CorrelationIds.MDC_KEY;
import static com.financebot.telegrambot.observability.CorrelationIds.currentOrCreate;

@Component
@RequiredArgsConstructor
public class FinanceTelegramBot implements LongPollingUpdateConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FinanceTelegramBot.class);

    private final TelegramBotProperties telegramBotProperties;
    private final TelegramCommandService telegramCommandService;
    private final TelegramClient telegramClient;
    private final FinanceBotMetrics metrics;
    private final TelegramMediaMessageHandler telegramMediaMessageHandler;
    private TelegramBotsLongPollingApplication botsApplication;

    @PostConstruct
    public void init() {
        try {
            this.botsApplication = new TelegramBotsLongPollingApplication();

            botsApplication.registerBot(telegramBotProperties.token(), this);

            LOGGER.info("Bot do Telegram iniciado com sucesso");
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
            if (update == null || !update.hasMessage()) {
                continue;
            }

            Message message = update.getMessage();
            if (!message.hasText() && !message.hasDocument()
                    && !message.hasPhoto() && !message.hasVoice() && !message.hasAudio()) {
                continue;
            }

            String chatId = message.getChatId().toString();
            Long telegramId = message.getChatId();
            String telegramUsername = message.getFrom() != null
                    ? message.getFrom().getUserName()
                    : null;
            String telegramFirstName = message.getFrom() != null
                    ? message.getFrom().getFirstName()
                    : null;

            MDC.put(MDC_KEY, currentOrCreate());
            try {
                String responseText;
                if (message.hasText()) {
                    LOGGER.info("Processando mensagem de texto recebida do Telegram");
                    responseText = telegramCommandService.handleMessage(
                            message.getText(), telegramId, telegramUsername, telegramFirstName);
                } else {
                    LOGGER.info("Processando mídia recebida do Telegram");
                    responseText = telegramMediaMessageHandler.handle(
                            message, telegramId, telegramUsername, telegramFirstName);
                }

                boolean delivered = sendMessage(chatId, responseText);
                metrics.recordTelegramMessage(delivered ? "success" : "delivery_failure");
                if (delivered) {
                    LOGGER.info("Mensagem do Telegram processada e respondida com sucesso");
                } else {
                    LOGGER.warn("Mensagem do Telegram processada, mas a resposta não foi entregue");
                }
            } catch (Exception e) {
                metrics.recordTelegramMessage("failure");
                LOGGER.error("Falha ao processar mensagem do Telegram: errorType={}",
                        e.getClass().getSimpleName());
                sendMessage(chatId, """
                        Não consegui processar sua mensagem agora.

                        Tente novamente em alguns instantes.
                        """);
            } finally {
                MDC.remove(MDC_KEY);
            }
        }
    }

    private boolean sendMessage(String chatId, String text) {
        try {
            SendMessage sendMessage = SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .parseMode("HTML")
                    .build();

            telegramClient.execute(sendMessage);
            return true;
        } catch (Exception e) {
            LOGGER.error("Falha ao enviar mensagem ao Telegram: errorType={}",
                    e.getClass().getSimpleName());
            return false;
        }
    }

}
