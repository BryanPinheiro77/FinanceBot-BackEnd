package com.financebot.telegrambot.bot;

import com.financebot.telegrambot.config.TelegramBotProperties;
import com.financebot.telegrambot.observability.FinanceBotMetrics;
import com.financebot.telegrambot.media.application.TelegramMediaMessageHandler;
import com.financebot.telegrambot.service.TelegramCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceTelegramBotTest {
    @Mock private TelegramCommandService commandService;
    @Mock private TelegramClient telegramClient;
    @Mock private FinanceBotMetrics metrics;
    @Mock private TelegramMediaMessageHandler mediaMessageHandler;

    private FinanceTelegramBot bot;

    @BeforeEach
    void setUp() {
        bot = new FinanceTelegramBot(
                new TelegramBotProperties("token", "financebot"),
                commandService,
                telegramClient,
                metrics,
                mediaMessageHandler
        );
    }

    @Test
    void recordsSuccessfulMessageDelivery() throws Exception {
        Update update = textUpdate();
        when(commandService.handleMessage("gastei 20", 123L, "bryan", "Bryan"))
                .thenReturn("Preview");

        bot.consume(List.of(update));

        verify(telegramClient).execute(any(SendMessage.class));
        verify(metrics).recordTelegramMessage("success");
    }

    @Test
    void recordsDeliveryFailure() throws Exception {
        Update update = textUpdate();
        when(commandService.handleMessage("gastei 20", 123L, "bryan", "Bryan"))
                .thenReturn("Preview");
        doThrow(new IllegalStateException("telegram unavailable"))
                .when(telegramClient).execute(any(SendMessage.class));

        bot.consume(List.of(update));

        verify(metrics).recordTelegramMessage("delivery_failure");
    }

    private Update textUpdate() {
        User user = new User(1L, "Bryan", false);
        user.setUserName("bryan");
        Message message = Message.builder()
                .chat(new Chat(123L, "private"))
                .from(user)
                .text("gastei 20")
                .build();
        Update update = new Update();
        update.setMessage(message);
        return update;
    }
}
